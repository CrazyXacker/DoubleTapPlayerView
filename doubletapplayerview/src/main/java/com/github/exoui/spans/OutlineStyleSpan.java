package com.github.exoui.spans;

import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class OutlineStyleSpan extends CharacterStyle {
  private Paint.Join join;
  private int strokeColor;
  private float strokeWidth;
  private Paint.Style style;

  public OutlineStyleSpan(int strokeColor, float strokeWidth) {
    super();
    this.join = Paint.Join.ROUND;
    this.strokeColor = strokeColor;
    this.strokeWidth = strokeWidth;
    this.style = Paint.Style.FILL_AND_STROKE;
  }

  public int getStrokeColor() {
    return strokeColor;
  }

  public float getStrokeWidth() {
    return strokeWidth;
  }

  public Paint.Join getJoin() {
    return join;
  }

  public void setJoin(Paint.Join join) {
    this.join = join;
  }


  public Paint.Style getStyle() {
    return style;
  }

  public void setStyle(Paint.Style style) {
    this.style = style;
  }

  @Override
  public void updateDrawState(TextPaint textPaint) {
  }
}