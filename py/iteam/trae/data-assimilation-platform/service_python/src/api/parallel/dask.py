# service_python/src/api/parallel/dask.py

import asyncio
import logging
from typing import Optional

logger = logging.getLogger(__name__)

class DaskClusterManager:
    """Dask集群管理器"""
    
    def __init__(self, n_workers: int = 4, threads_per_worker: int = 2):
        self.n_workers = n_workers
        self.threads_per_worker = threads_per_worker
        self.cluster = None
        self.client = None
    
    async def start(self):
        """启动Dask集群"""
        try:
            # 尝试导入Dask
            from dask.distributed import LocalCluster, Client
            
            logger.info(f"启动Dask本地集群，{self.n_workers}个工作节点，每个节点{self.threads_per_worker}线程")
            
            # 创建本地集群
            self.cluster = LocalCluster(
                n_workers=self.n_workers,
                threads_per_worker=self.threads_per_worker,
                processes=True,
                dashboard_address=':8787'
            )
            
            # 创建客户端
            self.client = Client(self.cluster)
            
            logger.info(f"Dask集群启动成功: {self.client.scheduler_info()['address']}")
            
        except ImportError:
            logger.warning("Dask未安装，使用单线程模式")
        except Exception as e:
            logger.error(f"启动Dask集群失败: {str(e)}")
    
    async def stop(self):
        """停止Dask集群"""
        try:
            if self.client:
                self.client.close()
            if self.cluster:
                self.cluster.close()
            logger.info("Dask集群已关闭")
        except Exception as e:
            logger.error(f"停止Dask集群失败: {str(e)}")
    
    def status(self) -> str:
        """获取集群状态"""
        if self.client and self.client.status == 'running':
            return 'running'
        return 'stopped'
    
    def get_client(self):
        """获取Dask客户端"""
        return self.client