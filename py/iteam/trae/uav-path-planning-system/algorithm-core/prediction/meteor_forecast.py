import torch
import torch.nn as nn
import xgboost as xgb
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score
import joblib
import os
from datetime import datetime
from typing import Dict, List, Tuple, Optional
import logging

logger = logging.getLogger(__name__)

class LSTM_Meteor(nn.Module):
    """
    LSTM气象特征提取模型
    """
    def __init__(self, input_dim=5, hidden_dim=64, num_layers=2, output_dim=3):
        super(LSTM_Meteor, self).__init__()
        self.hidden_dim = hidden_dim
        self.num_layers = num_layers
        self.lstm = nn.LSTM(input_dim, hidden_dim, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_dim, output_dim)  # 输出风速、风向、湍流订正量
    
    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_dim).to(x.device)
        c0 = torch.zeros(self.num_layers, x.size(0), self.hidden_dim).to(x.device)
        out, _ = self.lstm(x, (h0, c0))
        out = self.fc(out[:, -1, :])  # 取最后一个时间步输出
        return out

class MeteorCorrectionModel:
    """
    LSTM+XGBoost气象预测与订正模型
    """
    
    def __init__(self, 
                 input_dim=5, 
                 hidden_dim=64, 
                 num_layers=2, 
                 output_dim=3,
                 model_dir='models'):
        """
        初始化气象订正模型
        :param input_dim: 输入特征维度
        :param hidden_dim: LSTM隐藏层维度
        :param num_layers: LSTM层数
        :param output_dim: 输出维度
        :param model_dir: 模型保存目录
        """
        self.input_dim = input_dim
        self.hidden_dim = hidden_dim
        self.num_layers = num_layers
        self.output_dim = output_dim
        self.model_dir = model_dir
        self.lstm_model = None
        self.xgb_model = None
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self.scaler = None
        
        # 创建模型保存目录
        os.makedirs(self.model_dir, exist_ok=True)
    
    def prepare_data(self, historical_data: pd.DataFrame, sequence_length: int = 10):
        """
        准备训练数据
        :param historical_data: 历史气象数据
        :param sequence_length: 时间序列长度
        :return: 训练数据和标签
        """
        features = ['wind_speed', 'wind_dir', 'turbulence', 'visibility', 'thunder_risk']
        target_vars = ['wind_speed', 'wind_dir', 'turbulence']
        
        time_series = []
        labels = []
        
        for idx in range(sequence_length, len(historical_data)):
            # 取前sequence_length个时间步的气象特征作为输入
            seq = historical_data[features].iloc[idx-sequence_length:idx].values
            # 标签：真实观测值
            label = historical_data[target_vars].iloc[idx].values
            time_series.append(seq)
            labels.append(label)
        
        return np.array(time_series), np.array(labels)
    
    def train(self, 
              historical_data: pd.DataFrame, 
              wrf_data: pd.DataFrame, 
              epochs: int = 100,
              batch_size: int = 32,
              learning_rate: float = 0.001,
              sequence_length: int = 10):
        """
        训练模型
        :param historical_data: 历史观测数据
        :param wrf_data: WRF预测数据
        :param epochs: 训练轮数
        :param batch_size: 批次大小
        :param learning_rate: 学习率
        :param sequence_length: 时间序列长度
        :return: 训练结果
        """
        logger.info("开始训练LSTM+XGBoost气象订正模型")
        
        # 准备数据
        X_seq, y_true = self.prepare_data(historical_data, sequence_length)
        
        # 计算WRF预测与真实值的差值作为标签
        y_correction = []
        for i, idx in enumerate(range(sequence_length, len(historical_data))):
            wrf_pred = wrf_data[['wind_speed', 'wind_dir', 'turbulence']].iloc[idx].values
            correction = y_true[i] - wrf_pred
            y_correction.append(correction)
        y_correction = np.array(y_correction)
        
        # 划分训练集和测试集
        X_train, X_test, y_train, y_test = train_test_split(X_seq, y_correction, test_size=0.2, random_state=42)
        
        # 训练LSTM模型
        self.lstm_model = LSTM_Meteor(self.input_dim, self.hidden_dim, self.num_layers, self.output_dim).to(self.device)
        criterion = nn.MSELoss()
        optimizer = torch.optim.Adam(self.lstm_model.parameters(), lr=learning_rate)
        
        # 转换为Tensor
        X_train_tensor = torch.tensor(X_train, dtype=torch.float32).to(self.device)
        y_train_tensor = torch.tensor(y_train, dtype=torch.float32).to(self.device)
        X_test_tensor = torch.tensor(X_test, dtype=torch.float32).to(self.device)
        
        # LSTM训练
        for epoch in range(epochs):
            self.lstm_model.train()
            optimizer.zero_grad()
            outputs = self.lstm_model(X_train_tensor)
            loss = criterion(outputs, y_train_tensor)
            loss.backward()
            optimizer.step()
            
            if epoch % 10 == 0:
                logger.info(f'LSTM Epoch {epoch}, Loss: {loss.item():.4f}')
        
        # 提取LSTM特征
        self.lstm_model.eval()
        with torch.no_grad():
            lstm_features_train = self.lstm_model(X_train_tensor).cpu().numpy()
            lstm_features_test = self.lstm_model(X_test_tensor).cpu().numpy()
        
        # 构建XGBoost输入
        X_xgb_train = np.hstack([lstm_features_train, X_train[:, -1, :]])  # LSTM特征 + 最后一步原始特征
        X_xgb_test = np.hstack([lstm_features_test, X_test[:, -1, :]])
        
        # 训练XGBoost模型
        self.xgb_model = xgb.XGBRegressor(n_estimators=100, max_depth=5, learning_rate=0.1, objective='reg:squarederror')
        self.xgb_model.fit(X_xgb_train, y_train)
        
        # 模型评估
        y_pred = self.xgb_model.predict(X_xgb_test)
        mse = mean_squared_error(y_test, y_pred)
        r2 = r2_score(y_test, y_pred)
        
        logger.info(f'XGBoost MSE: {mse:.4f}, R2: {r2:.4f}')
        
        # 保存模型
        self.save_model()
        
        return {'mse': mse, 'r2': r2}
    
    def predict(self, wrf_data: Dict[str, np.ndarray], sequence_length: int = 10):
        """
        预测并订正气象数据
        :param wrf_data: WRF原始输出
        :param sequence_length: 时间序列长度
        :return: 订正后的气象数据
        """
        if not self.lstm_model or not self.xgb_model:
            raise ValueError("模型未训练，请先调用train方法")
        
        # 提取特征
        features = ['wind_speed', 'wind_dir', 'turbulence', 'visibility', 'thunder_risk']
        wrf_seq = np.array([wrf_data[feature] for feature in features]).T
        wrf_seq = wrf_seq[-sequence_length:, :]  # 取最近的sequence_length个时间步
        
        # 转换为Tensor
        wrf_tensor = torch.tensor(wrf_seq.reshape(1, sequence_length, self.input_dim), dtype=torch.float32).to(self.device)
        
        # LSTM特征提取
        self.lstm_model.eval()
        with torch.no_grad():
            lstm_feature = self.lstm_model(wrf_tensor).cpu().numpy()
        
        # XGBoost预测
        xgb_input = np.hstack([lstm_feature, wrf_seq[-1, :].reshape(1, -1)])
        correction = self.xgb_model.predict(xgb_input)[0]
        
        # 应用订正量
        corrected_data = wrf_data.copy()
        corrected_data['wind_speed'] += correction[0]
        corrected_data['wind_dir'] += correction[1]
        corrected_data['turbulence'] += correction[2]
        
        # 确保物理合理性
        corrected_data['wind_speed'] = np.maximum(corrected_data['wind_speed'], 0)
        corrected_data['wind_dir'] = np.mod(corrected_data['wind_dir'], 360)
        corrected_data['turbulence'] = np.clip(corrected_data['turbulence'], 0, 1)
        
        return corrected_data
    
    def batch_predict(self, wrf_data_list: List[Dict[str, np.ndarray]], sequence_length: int = 10):
        """
        批量预测
        :param wrf_data_list: WRF数据列表
        :param sequence_length: 时间序列长度
        :return: 订正后的气象数据列表
        """
        results = []
        for wrf_data in wrf_data_list:
            corrected = self.predict(wrf_data, sequence_length)
            results.append(corrected)
        return results
    
    def save_model(self, model_name: Optional[str] = None):
        """
        保存模型
        :param model_name: 模型名称
        """
        if not model_name:
            model_name = f'meteor_correction_model_{datetime.now().strftime("%Y%m%d_%H%M%S")}'
        
        # 保存LSTM模型
        lstm_path = os.path.join(self.model_dir, f'{model_name}_lstm.pth')
        torch.save(self.lstm_model.state_dict(), lstm_path)
        
        # 保存XGBoost模型
        xgb_path = os.path.join(self.model_dir, f'{model_name}_xgb.joblib')
        joblib.dump(self.xgb_model, xgb_path)
        
        logger.info(f'模型保存成功: {lstm_path}, {xgb_path}')
    
    def load_model(self, model_name: str):
        """
        加载模型
        :param model_name: 模型名称
        """
        # 加载LSTM模型
        lstm_path = os.path.join(self.model_dir, f'{model_name}_lstm.pth')
        if os.path.exists(lstm_path):
            self.lstm_model = LSTM_Meteor(self.input_dim, self.hidden_dim, self.num_layers, self.output_dim).to(self.device)
            self.lstm_model.load_state_dict(torch.load(lstm_path, map_location=self.device))
            logger.info(f'LSTM模型加载成功: {lstm_path}')
        else:
            raise FileNotFoundError(f'LSTM模型文件不存在: {lstm_path}')
        
        # 加载XGBoost模型
        xgb_path = os.path.join(self.model_dir, f'{model_name}_xgb.joblib')
        if os.path.exists(xgb_path):
            self.xgb_model = joblib.load(xgb_path)
            logger.info(f'XGBoost模型加载成功: {xgb_path}')
        else:
            raise FileNotFoundError(f'XGBoost模型文件不存在: {xgb_path}')
    
    def evaluate(self, test_data: pd.DataFrame, wrf_data: pd.DataFrame, sequence_length: int = 10):
        """
        评估模型性能
        :param test_data: 测试数据
        :param wrf_data: WRF预测数据
        :param sequence_length: 时间序列长度
        :return: 评估指标
        """
        if not self.lstm_model or not self.xgb_model:
            raise ValueError("模型未训练，请先调用train方法")
        
        # 准备测试数据
        X_seq, y_true = self.prepare_data(test_data, sequence_length)
        
        # 计算WRF预测与真实值的差值作为标签
        y_correction = []
        for i, idx in enumerate(range(sequence_length, len(test_data))):
            wrf_pred = wrf_data[['wind_speed', 'wind_dir', 'turbulence']].iloc[idx].values
            correction = y_true[i] - wrf_pred
            y_correction.append(correction)
        y_correction = np.array(y_correction)
        
        # 提取LSTM特征
        X_tensor = torch.tensor(X_seq, dtype=torch.float32).to(self.device)
        self.lstm_model.eval()
        with torch.no_grad():
            lstm_features = self.lstm_model(X_tensor).cpu().numpy()
        
        # XGBoost预测
        X_xgb = np.hstack([lstm_features, X_seq[:, -1, :]])
        y_pred = self.xgb_model.predict(X_xgb)
        
        # 计算评估指标
        mse = mean_squared_error(y_correction, y_pred)
        r2 = r2_score(y_correction, y_pred)
        
        # 计算WRF原始误差和订正后误差
        wrf_error = np.mean(np.abs(y_correction))
        corrected_error = np.mean(np.abs(y_correction - y_pred))
        improvement = (wrf_error - corrected_error) / wrf_error * 100
        
        logger.info(f'模型评估结果: MSE={mse:.4f}, R2={r2:.4f}, 误差改善={improvement:.2f}%')
        
        return {
            'mse': mse,
            'r2': r2,
            'wrf_error': wrf_error,
            'corrected_error': corrected_error,
            'improvement': improvement
        }

# 工具函数
def train_meteor_correction_model(historical_data: pd.DataFrame, 
                                wrf_data: pd.DataFrame, 
                                model_dir: str = 'models') -> MeteorCorrectionModel:
    """
    训练气象订正模型
    :param historical_data: 历史观测数据
    :param wrf_data: WRF预测数据
    :param model_dir: 模型保存目录
    :return: 训练好的模型
    """
    model = MeteorCorrectionModel(model_dir=model_dir)
    model.train(historical_data, wrf_data)
    return model

def correct_meteor_data(wrf_data: Dict[str, np.ndarray], 
                       model_path: str) -> Dict[str, np.ndarray]:
    """
    订正气象数据
    :param wrf_data: WRF原始数据
    :param model_path: 模型路径
    :return: 订正后的数据
    """
    model = MeteorCorrectionModel()
    model_name = os.path.basename(model_path).split('_lstm.pth')[0]
    model.load_model(model_name)
    return model.predict(wrf_data)

def generate_synthetic_data(n_samples: int = 1000) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    生成合成气象数据用于测试
    :param n_samples: 样本数量
    :return: 历史观测数据和WRF预测数据
    """
    np.random.seed(42)
    
    # 生成时间序列
    times = pd.date_range('2024-01-01', periods=n_samples, freq='H')
    
    # 生成真实气象数据
    wind_speed = np.random.normal(5, 2, n_samples)
    wind_dir = np.random.uniform(0, 360, n_samples)
    turbulence = np.random.beta(2, 5, n_samples)
    visibility = np.random.normal(8000, 2000, n_samples)
    thunder_risk = np.random.beta(1, 10, n_samples)
    
    historical_data = pd.DataFrame({
        'time': times,
        'wind_speed': wind_speed,
        'wind_dir': wind_dir,
        'turbulence': turbulence,
        'visibility': visibility,
        'thunder_risk': thunder_risk
    })
    
    # 生成WRF预测数据（添加误差）
    wrf_data = pd.DataFrame({
        'time': times,
        'wind_speed': wind_speed + np.random.normal(0, 0.5, n_samples),
        'wind_dir': wind_dir + np.random.normal(0, 10, n_samples),
        'turbulence': turbulence + np.random.normal(0, 0.05, n_samples),
        'visibility': visibility + np.random.normal(0, 500, n_samples),
        'thunder_risk': thunder_risk + np.random.normal(0, 0.02, n_samples)
    })
    
    return historical_data, wrf_data

if __name__ == "__main__":
    # 示例使用
    print("LSTM+XGBoost气象预测与订正模型示例")
    
    # 生成合成数据
    historical_data, wrf_data = generate_synthetic_data(1000)
    print(f"生成数据完成，样本数量: {len(historical_data)}")
    
    # 训练模型
    model = MeteorCorrectionModel(model_dir='./models')
    train_result = model.train(historical_data, wrf_data, epochs=50)
    print(f"训练完成，MSE: {train_result['mse']:.4f}, R2: {train_result['r2']:.4f}")
    
    # 评估模型
    eval_result = model.evaluate(historical_data, wrf_data)
    print(f"模型评估: 误差改善 {eval_result['improvement']:.2f}%")
    
    # 测试预测
    test_wrf_data = {
        'wind_speed': np.array([4.5, 4.6, 4.7, 4.8, 4.9, 5.0, 5.1, 5.2, 5.3, 5.4]),
        'wind_dir': np.array([90, 91, 92, 93, 94, 95, 96, 97, 98, 99]),
        'turbulence': np.array([0.1, 0.11, 0.12, 0.13, 0.14, 0.15, 0.16, 0.17, 0.18, 0.19]),
        'visibility': np.array([8000, 8100, 8200, 8300, 8400, 8500, 8600, 8700, 8800, 8900]),
        'thunder_risk': np.array([0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1])
    }
    
    corrected = model.predict(test_wrf_data)
    print("\n预测结果:")
    print(f"原始风速: {test_wrf_data['wind_speed'][-1]:.2f} m/s")
    print(f"订正风速: {corrected['wind_speed']:.2f} m/s")
    print(f"原始风向: {test_wrf_data['wind_dir'][-1]:.2f}°")
    print(f"订正风向: {corrected['wind_dir']:.2f}°")
    print(f"原始湍流: {test_wrf_data['turbulence'][-1]:.2f}")
    print(f"订正湍流: {corrected['turbulence']:.2f}")
    
    # 保存模型
    model.save_model('test_model')
    print("\n模型保存成功！")
    
    # 加载模型
    new_model = MeteorCorrectionModel()
    new_model.load_model('test_model')
    print("模型加载成功！")
    
    # 测试加载的模型
    corrected2 = new_model.predict(test_wrf_data)
    print("\n加载模型预测结果:")
    print(f"订正风速: {corrected2['wind_speed']:.2f} m/s")
    print(f"订正风向: {corrected2['wind_dir']:.2f}°")
    print(f"订正湍流: {corrected2['turbulence']:.2f}")
    
    print("\nLSTM+XGBoost气象预测与订正模型测试完成！")