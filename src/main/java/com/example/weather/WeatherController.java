package com.example.weather;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    @GetMapping("/weather")
    public String getWeather() {
        return "成都当前气象：风速 3.2m/s，温度 22℃，湿度 65%，LSTM预测明日风速 4.1m/s";
    }
}