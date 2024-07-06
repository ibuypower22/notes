package com.example.notes;

import java.time.LocalDate;

public class Note {
    private int id;
    private String text;
    private LocalDate date;
    private boolean pinned;
    private int originalIndex;
    private String title;
    public  Note () {};

    public Note(int id, String text, LocalDate date, boolean pinned, int originalIndex, String title) {
        this.id = id;
        this.text = text;
        this.date = date;
        this.pinned = pinned;
        this.originalIndex = originalIndex;
        this.title = title;
    }

    public Note(String text, LocalDate date, boolean pinned, int originalIndex, String title) {
        this(0, text, date, pinned, originalIndex, title);
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(int originalIndex) {
        this.originalIndex = originalIndex;
    }

}