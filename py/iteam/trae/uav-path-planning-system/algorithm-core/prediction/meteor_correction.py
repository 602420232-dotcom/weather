import torch
import torch.nn as nn
import xgboost as xgb
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

# 1. LSTM模型（时序气象特征提取）
class LSTM_Meteor(nn.Module):
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

# 2. LSTM+XGBoost联合订正流程
def meteor_correction_model(wrf_data, historical_data):
    """
    基于LSTM+XGBoost对WRF输出进行订正
    :param wrf_data: WRF原始输出（标准化后）
    :param historical_data: 历史气象观测数据（pd.DataFrame）
    :return: 订正后的气象数据
    """
    # 步骤1：构造LSTM训练数据（时序特征）
    time_series = []
    labels = []
    for idx in range(10, len(historical_data)):
        # 取前10个时间步的气象特征作为输入
        seq = historical_data[['wind_speed', 'wind_dir', 'turbulence', 'visibility', 'thunder_risk']].iloc[idx-10:idx].values
        # 标签：真实观测值与WRF预测值的差值（订正量）
        label = historical_data[['wind_speed', 'wind_dir', 'turbulence']].iloc[idx].values - wrf_data[['wind_speed', 'wind_dir', 'turbulence']].iloc[idx].values
        time_series.append(seq)
        labels.append(label)
    
    time_series = np.array(time_series)
    labels = np.array(labels)
    
    # 步骤2：LSTM训练（提取时序特征）
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    lstm_model = LSTM_Meteor().to(device)
    criterion = nn.MSELoss()
    optimizer = torch.optim.Adam(lstm_model.parameters(), lr=0.001)
    
    # 数据转换为Tensor
    X_tensor = torch.tensor(time_series, dtype=torch.float32).to(device)
    y_tensor = torch.tensor(labels, dtype=torch.float32).to(device)
    
    # LSTM训练
    for epoch in range(100):
        lstm_model.train()
        optimizer.zero_grad()
        outputs = lstm_model(X_tensor)
        loss = criterion(outputs, y_tensor)
        loss.backward()
        optimizer.step()
        if epoch % 10 == 0:
            print(f'LSTM Epoch {epoch}, Loss: {loss.item():.4f}')
    
    # 步骤3：提取LSTM特征作为XGBoost输入
    lstm_model.eval()
    with torch.no_grad():
        lstm_features = lstm_model(X_tensor).cpu().numpy()
    
    # 步骤4：XGBoost训练（最终订正）
    X_xgb = np.hstack([lstm_features, time_series[:, -1, :]])  # LSTM特征 + 最后一步原始特征
    X_train, X_test, y_train, y_test = train_test_split(X_xgb, labels, test_size=0.2, random_state=42)
    
    xgb_model = xgb.XGBRegressor(n_estimators=100, max_depth=5, learning_rate=0.1)
    xgb_model.fit(X_train, y_train)
    
    # 步骤5：模型评估
    y_pred = xgb_model.predict(X_test)
    mse = mean_squared_error(y_test, y_pred)
    print(f'XGBoost MSE: {mse:.4f}')
    
    # 步骤6：对WRF数据进行订正
    wrf_seq = np.array([wrf_data[['wind_speed', 'wind_dir', 'turbulence', 'visibility', 'thunder_risk']].values])
    wrf_tensor = torch.tensor(wrf_seq, dtype=torch.float32).to(device)
    with torch.no_grad():
        lstm_feature = lstm_model(wrf_tensor).cpu().numpy()
    xgb_input = np.hstack([lstm_feature, wrf_seq[:, -1, :]])
    correction = xgb_model.predict(xgb_input)[0]
    
    # 应用订正量
    corrected_data = wrf_data.copy()
    corrected_data['wind_speed'] += correction[0]
    corrected_data['wind_dir'] += correction[1]
    corrected_data['turbulence'] += correction[2]
    
    # 贝叶斯同化：添加不确定性方差场
    corrected_data['uncertainty'] = np.random.normal(0, 0.05, size=corrected_data['wind_speed'].shape)  # 示例方差场
    
    return corrected_data