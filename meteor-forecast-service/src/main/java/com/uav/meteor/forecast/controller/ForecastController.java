package com.uav.meteor.forecast.controller;
import com.uav.common.dto.ForecastRequest;
import com.uav.common.utils.PythonScriptInvoker;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final PythonScriptInvoker pythonScriptInvoker;

    @Value("${forecast.python-script}")
    private String pythonScriptPath;

    public ForecastController(PythonScriptInvoker pythonScriptInvoker) {
        this.pythonScriptInvoker = pythonScriptInvoker;
    }

    @PostMapping("/predict")
    public Map<String, Object> predict(@Valid @RequestBody ForecastRequest request) {
        Map<String, Object> params = Map.of(
            "method", request.getMethod(),
            "data", request.getData(),
            "config", request.getConfig()
        );
        return pythonScriptInvoker.execute(pythonScriptPath, "predict", params);
    }

    @PostMapping("/correct")
    public Map<String, Object> correct(@Valid @RequestBody ForecastRequest request) {
        Map<String, Object> params = Map.of(
            "method", request.getMethod(),
            "data", request.getData(),
            "config", request.getConfig()
        );
        return pythonScriptInvoker.execute(pythonScriptPath, "correct", params);
    }

    @GetMapping("/models")
    public Map<String, Object> getModels() {
        return Map.of(
            "success", true,
            "data", Map.of("lstm_model", "LSTM时间序列预测模型", "xgb_model", "XGBoost数据订正模型")
        );
    }

    @GetMapping("/get")
    public Map<String, Object> getForecast(@RequestParam Double lat, @RequestParam Double lng, @RequestParam Integer hours) {
        Map<String, Object> params = Map.of(
            "lat", lat,
            "lng", lng,
            "hours", hours
        );
        return pythonScriptInvoker.execute(pythonScriptPath, "get_forecast", params);
    }

    @PostMapping("/detail")
    public Map<String, Object> getDetailedForecast(@RequestBody Map<String, Object> request) {
        return pythonScriptInvoker.execute(pythonScriptPath, "get_detailed_forecast", request);
    }

    @GetMapping("/realtime")
    public Map<String, Object> getRealtimeWeather(@RequestParam Double lat, @RequestParam Double lng) {
        Map<String, Object> params = Map.of(
            "lat", lat,
            "lng", lng
        );
        return pythonScriptInvoker.execute(pythonScriptPath, "get_realtime_weather", params);
    }
}
