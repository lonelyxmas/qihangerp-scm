package cn.qihang.ai.assistant.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TodoService {

    public TodoService() {
    }

    public static class Todos {
        public List<String> high = new ArrayList<>();
        public List<String> mid = new ArrayList<>();
        public List<String> low = new ArrayList<>();
        public List<String> daily = new ArrayList<>();
        public List<String> temp = new ArrayList<>();
    }

    public Todos parse() {
        return new Todos();
    }

    public void clearTempReminders() {
    }
}