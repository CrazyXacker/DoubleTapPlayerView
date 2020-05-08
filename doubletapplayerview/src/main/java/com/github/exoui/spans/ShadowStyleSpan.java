package com.github.exoui.spans;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class ShadowStyleSpan extends CharacterStyle {

  private int radius;
  private int dx;
  private int dy;
  private int color;

  public ShadowStyleSpan(int radius, int dx, int dy, int color) {
    super();
    this.radius = radius;
    this.dx = dx;
    this.dy = dy;
    this.color = color;
  }

  public int getRadius() {
    return radius;
  }

  public int getDx() {
    return dx;
  }

  public int getDy() {
    return dy;
  }

  public int getColor() {
    return color;
  }

  @Override
  public void updateDrawState(TextPaint textPaint) {
  }
}
