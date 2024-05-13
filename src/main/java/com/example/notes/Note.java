package com.example.notes;

import java.time.LocalDate;

public class Note {
    private int id;
    private String text;
    private LocalDate date;
    private boolean pinned;
    private String fontFamily;
    private double fontSize;
    private boolean bold;
    private boolean italic;
    private String imagePath;
    private int originalIndex;


    public Note() {
    }

    public Note(int id, String text, LocalDate date, boolean pinned, String fontFamily, double fontSize, boolean bold, boolean italic, String imagePath, int originalIndex) {
        this.id = id;
        this.text = text;
        this.date = date;
        this.pinned = pinned;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
        this.imagePath = imagePath;
        this.originalIndex = originalIndex;
    }

    public Note(String text, LocalDate date, boolean pinned, String fontFamily, double fontSize, boolean bold, boolean italic, String imagePath, int originalIndex) {
        this(0, text, date, pinned, fontFamily, fontSize, bold, italic, imagePath, originalIndex);
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

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(int originalIndex) {
        this.originalIndex = originalIndex;
    }
}