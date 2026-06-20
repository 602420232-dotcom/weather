"""
Edge-Cloud Coordinator Service API
Provides REST interfaces for other services to call
"""
import logging
import os
from typing import Dict, List, Optional

from fastapi import Depends, FastAPI, HTTPException, BackgroundTasks, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from pydantic import BaseModel, Field

try:
    import numpy as np
    HAS_FASTAPI = True
except ImportError:
    HAS_FASTAPI = False
    logging.warning("FastAPI not installed. Running in demo mode.")

from coordinator import EdgeCloudCoordinator, EdgeTask
from federated_learning import FederatedLearning, DroneClient
from websocket_sync import WebSocketSync

logger = logging.getLogger(__name__)


if HAS_FASTAPI:
    app = FastAPI(
        title="Edge-Cloud Coordinator API",
        description="Edge-Cloud Coordination Framework API",
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc"
    )

    environment = os.environ.get("ENVIRONMENT", "development")
    cors_origins_str = os.environ.get("CORS_ORIGINS")

    if environment == "production":
        if not cors_origins_str:
            raise ValueError(
                "CORS_ORIGINS environment variable must be set in production environment. "
                "Example: CORS_ORIGINS=https://example.com,https://api.example.com"
            )
        cors_origins = cors_origins_str.split(",")
        allow_credentials = True
        logger.info("Production CORS configured for origins: %s", cors_origins)
    else:
        cors_origins = cors_origins_str.split(",") if cors_origins_str else ["*"]
        allow_credentials = False if cors_origins == ["*"] else True
        if cors_origins == ["*"]:
            logger.warning(
                "CORS is configured to allow all origins (*). "
                "This is acceptable for development but should be restricted in production."
            )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=cors_origins,
        allow_credentials=allow_credentials,
        allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Requested-With"],
    )


# ============================================================================
# Dependency Injection Provider Functions
# ============================================================================

def get_coordinator() -> EdgeCloudCoordinator:
    """
    Get coordinator instance

    Environment variables:
    - NODE_ID: Node identifier (default: edge_001)
    - SYNC_INTERVAL: Sync interval in seconds (default: 5.0)
    """
    return EdgeCloudCoordinator(
        node_id=os.environ.get("NODE_ID", "edge_001"),
        sync_interval=float(os.environ.get("SYNC_INTERVAL", "5.0"))
    )


def get_federated_learning() -> FederatedLearning:
    """
    Get federated learning instance

    Environment variables:
    - FL_STRATEGY: Aggregation strategy (default: fedavg)
    - FL_MIN_CLIENTS: Minimum client count (default: 2)
    """
    return FederatedLearning(
        aggregation_strategy=os.environ.get("FL_STRATEGY", "fedavg"),
        min_clients=int(os.environ.get("FL_MIN_CLIENTS", "2"))
    )


def get_websocket_sync() -> WebSocketSync:
    """Get WebSocket sync instance"""
    return WebSocketSync(node_id="coordinator")


class TaskSubmitRequest(BaseModel):
    """Task submission request"""
    task_type: str = Field(
        ...,
        description=(
            "Task type: global_path, local_avoidance, sensor_fusion, "
            "model_update, batch_processing"
        ),
    )
    priority: int = Field(default=5, ge=1, le=10, description="Priority 1-10")
    data: Dict = Field(default_factory=dict, description="Task data")
    deadline: float = Field(default=60.0, description="Deadline in seconds")


class TaskSubmitResponse(BaseModel):
    """Task submission response"""
    task_id: str
    status: str
    message: str


class TaskStatusResponse(BaseModel):
    """Task status query response"""
    task_id: str
    task_type: str
    priority: int
    status: str
    result: Optional[Dict] = None


class SystemStatusResponse(BaseModel):
    """System status response"""
    node_id: str
    queue_size: int
    completed_count: int
    cloud_connected: bool
    edge_connected: bool
    buffer_size: int


class FLClientUpdateRequest(BaseModel):
    """Federated learning client update request"""
    drone_id: str
    weights: Dict[str, List[List[float]]]
    n_samples: int
    metrics: Dict


class FLClientUpdateResponse(BaseModel):
    """Federated learning client update response"""
    aggregated: bool
    round_id: int
    global_accuracy: Optional[float] = None


class FLStatusResponse(BaseModel):
    """Federated learning status response"""
    strategy: str
    min_clients: int
    round_id: int
    clients_this_round: int
    global_accuracy: Optional[float] = None
    total_rounds: int


class FLTrainRequest(BaseModel):
    """Federated learning training request"""
    drone_id: str
    epochs: int = 5
    n_samples: int = 100


if HAS_FASTAPI:

    @app.get("/", tags=["Health"])
    async def root():
        """API root path"""
        return {
            "service": "Edge-Cloud Coordinator",
            "version": "1.0.0",
            "status": "running"
        }

    @app.get("/health", tags=["Health"])
    async def health_check():
        """Health check"""
        return {"status": "healthy"}

    @app.post("/tasks", response_model=TaskSubmitResponse, tags=["Tasks"])
    async def submit_task(
        request: TaskSubmitRequest,
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """
        Submit task

        Automatically assign to cloud or edge processing based on task type
        Uses dependency injection to get coordinator instance, supports test replacement
        """
        try:
            task_type = request.task_type

            task = EdgeTask(
                task_id=f"task_{len(coordinator.task_queue) + 1}",
                task_type=task_type,
                priority=request.priority,
                data=request.data,
                deadline=request.deadline
            )

            task_id = await coordinator.submit_task(task)
            coordinator.process_task(task)

            return TaskSubmitResponse(
                task_id=task_id,
                status="submitted",
                message=f"Task submitted to {request.task_type} queue"
            )

        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid task type")
        except Exception:
            logger.error("Task submission failed", exc_info=True)
            raise HTTPException(status_code=500, detail="Internal server error")

    @app.get("/tasks/{task_id}", response_model=TaskStatusResponse, tags=["Tasks"])
    async def get_task_status(
        task_id: str,
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Query task status"""
        for task in coordinator.task_queue:
            if task.task_id == task_id:
                return TaskStatusResponse(
                    task_id=task.task_id,
                    task_type=task.task_type,
                    priority=task.priority,
                    status=task.status
                )

        for task in coordinator.completed_tasks:
            if task.get("task_id") == task_id:
                return TaskStatusResponse(
                    task_id=task["task_id"],
                    task_type=task["task_type"],
                    priority=task.get("priority", 5),
                    status=task.get("status", "unknown"),
                    result=task.get("data", {}).get("result")
                )

        raise HTTPException(status_code=404, detail=f"Task {task_id} not found")

    @app.delete("/tasks/{task_id}", tags=["Tasks"])
    async def cancel_task(
        task_id: str,
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Cancel task"""
        for i, task in enumerate(coordinator.task_queue):
            if task.task_id == task_id:
                coordinator.task_queue.pop(i)
                return {"message": f"Task {task_id} cancelled"}

        raise HTTPException(status_code=404, detail=f"Task {task_id} not found")

    @app.get("/tasks", response_model=List[TaskStatusResponse], tags=["Tasks"])
    async def list_tasks(
        status: Optional[str] = None,
        limit: int = 10,
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Get task list"""
        if status == "completed":
            source = coordinator.completed_tasks[:limit]
            return [
                TaskStatusResponse(
                    task_id=t["task_id"],
                    task_type=t["task_type"],
                    priority=t.get("priority", 5),
                    status=t.get("status", "unknown"),
                )
                for t in source
            ]

        source = coordinator.task_queue[:limit]
        return [
            TaskStatusResponse(
                task_id=task.task_id,
                task_type=task.task_type,
                priority=task.priority,
                status=task.status
            )
            for task in source
        ]

    @app.get("/status", response_model=SystemStatusResponse, tags=["Coordinator"])
    async def get_system_status(
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Get system status"""
        return SystemStatusResponse(
            node_id=coordinator.node_id,
            queue_size=len(coordinator.task_queue),
            completed_count=len(coordinator.completed_tasks),
            cloud_connected=True,
            edge_connected=True,
            buffer_size=len(coordinator.offline_buffer)
        )

    @app.post("/sync", tags=["Coordinator"])
    async def sync_with_cloud(
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Sync cloud models"""
        try:
            await coordinator.sync_cloud_models()
            return {
                "message": "Cloud sync completed",
                "models": list(coordinator.cloud_models.keys())
            }
        except Exception:
            logger.error("Cloud sync failed", exc_info=True)
            raise HTTPException(status_code=500, detail="Internal server error")

    @app.post("/upload", tags=["Coordinator"])
    async def upload_edge_data(
        background_tasks: BackgroundTasks,
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Upload edge data to cloud"""

        def _upload():
            # Use asyncio to run async method in background
            import asyncio
            try:
                loop = asyncio.get_running_loop()
                loop.create_task(coordinator.upload_edge_data())
            except RuntimeError:
                asyncio.run(coordinator.upload_edge_data())

        background_tasks.add_task(_upload)
        return {"message": "Data upload task submitted"}

    @app.get("/models", tags=["Coordinator"])
    async def list_models(
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """List available models"""
        return {
            "cloud_models": list(coordinator.cloud_models.keys()),
            "local_models": list(coordinator.local_models.keys())
        }

    @app.post("/tasks/batch", tags=["Batch"])
    async def submit_batch_tasks(
        tasks: List[TaskSubmitRequest],
        coordinator: EdgeCloudCoordinator = Depends(get_coordinator)
    ):
        """Batch submit tasks (max 100)"""
        if len(tasks) > 100:
            raise HTTPException(status_code=400, detail="Batch task count cannot exceed 100")
        results = []
        for req in tasks:
            try:
                task_type = req.task_type
                task = EdgeTask(
                    task_id=f"task_{len(coordinator.task_queue) + 1}",
                    task_type=task_type,
                    priority=req.priority,
                    data=req.data,
                    deadline=req.deadline
                )
                task_id = await coordinator.submit_task(task)
                results.append({"task_id": task_id, "status": "submitted"})
            except Exception:
                logger.error("Batch task submission failed", exc_info=True)
                results.append({"error": "Task submission failed"})

        return {"results": results}

    @app.post(
        "/fl/update",
        response_model=FLClientUpdateResponse,
        tags=["Federated Learning"],
    )
    async def fl_client_update(
        request: FLClientUpdateRequest,
        federated_learning: FederatedLearning = Depends(get_federated_learning)
    ):
        """Receive federated learning client update"""
        np_weights = {k: np.array(v) for k, v in request.weights.items()}
        aggregated = federated_learning.receive_update(
            drone_id=request.drone_id,
            weights=np_weights,
            n_samples=request.n_samples,
            metrics=request.metrics
        )
        global_model = federated_learning.get_global_model()
        return FLClientUpdateResponse(
            aggregated=aggregated,
            round_id=federated_learning.round_id,
            global_accuracy=global_model.accuracy if global_model else None
        )

    @app.get("/fl/status", response_model=FLStatusResponse, tags=["Federated Learning"])
    async def fl_status(
        federated_learning: FederatedLearning = Depends(get_federated_learning)
    ):
        """Get federated learning status"""
        global_model = federated_learning.get_global_model()
        return FLStatusResponse(
            strategy=federated_learning.strategy,
            min_clients=federated_learning.min_clients,
            round_id=federated_learning.round_id,
            clients_this_round=len(federated_learning.client_updates),
            global_accuracy=global_model.accuracy if global_model else None,
            total_rounds=len(federated_learning.round_history)
        )

    @app.get("/fl/history", tags=["Federated Learning"])
    async def fl_history(
        federated_learning: FederatedLearning = Depends(get_federated_learning)
    ):
        """Get federated learning history"""
        return {"rounds": federated_learning.round_history}

    @app.post("/fl/train", tags=["Federated Learning"])
    async def fl_local_train(
        request: FLTrainRequest,
        federated_learning: FederatedLearning = Depends(get_federated_learning)
    ):
        """Simulate drone local training"""
        global_model = federated_learning.get_global_model()
        if global_model is None:
            client = DroneClient(request.drone_id)
            dummy_weights = {"w": np.array([1.0, 2.0, 3.0]), "b": np.array([0.0])}
            updated, n_samples, metrics = client.local_train(
                dummy_weights, request.epochs
            )
        else:
            client = DroneClient(request.drone_id)
            updated, n_samples, metrics = client.local_train(
                global_model.weights, request.epochs
            )
        aggregated = federated_learning.receive_update(
            drone_id=request.drone_id,
            weights=updated,
            n_samples=n_samples,
            metrics=metrics
        )
        return {
            "drone_id": request.drone_id,
            "n_samples": n_samples,
            "metrics": metrics,
            "aggregated": aggregated,
        }

    @app.websocket("/ws/{drone_id}")
    async def websocket_endpoint(
        websocket: WebSocket,
        drone_id: str,
        ws_sync: WebSocketSync = Depends(get_websocket_sync)
    ):
        """
        WebSocket real-time communication endpoint

        Features:
        - Server sends ping every 30s, client must reply with pong
        - No heartbeat for 60s marks connection as expired, auto disconnect
        - Supports auto reconnect (identified by drone_id)
        - Check connection health via /ws/status
        """
        await websocket.accept()
        await ws_sync.connect(drone_id, websocket)

        try:
            while True:
                data = await websocket.receive_json()
                await ws_sync.handle_message(drone_id, data)

        except WebSocketDisconnect:
            logger.info(f"WebSocket client disconnected: {drone_id}")
            await ws_sync.disconnect(drone_id, reason="client_disconnect")

        except Exception as e:
            logger.error(f"WebSocket exception ({drone_id}): {e}")
            await ws_sync.disconnect(drone_id, reason="error")

    @app.get("/metrics", tags=["Monitoring"])
    async def prometheus_metrics():
        """Prometheus metrics exposure endpoint"""
        try:
            from prometheus_client import generate_latest, CONTENT_TYPE_LATEST
            data = generate_latest()
            return Response(content=data, media_type=CONTENT_TYPE_LATEST)
        except ImportError:
            return {"error": "prometheus_client not installed, metrics unavailable"}
        except Exception as e:
            logger.error("Prometheus metrics generation failed: %s", e)
            return {"error": "Metrics generation failed"}

    @app.get("/ws/status", tags=["WebSocket"])
    async def websocket_status(
        ws_sync: WebSocketSync = Depends(get_websocket_sync)
    ):
        """WebSocket connection health status"""
        return await ws_sync.get_health_status()


def run_server(host: str = "0.0.0.0", port: int = 8000):
    """Start API server"""
    if not HAS_FASTAPI:
        logger.info("[Error] FastAPI is required. Install with: pip install fastapi uvicorn")
        return

    import uvicorn
    uvicorn.run(app, host=host, port=port)


if __name__ == "__main__":
    run_server()
