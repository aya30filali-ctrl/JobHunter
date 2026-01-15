package com.jobapp.model;

import java.util.List;
import java.util.ArrayList;

public class DataList<T> {
    private List<T> items = new ArrayList<>();

    public void add(T item)
    { items.add(item); }

    public List<T> getAll()
    { return items; }

    public int count() {
        return items.size(); }

    public int size() {
        return items.size();
    }
}