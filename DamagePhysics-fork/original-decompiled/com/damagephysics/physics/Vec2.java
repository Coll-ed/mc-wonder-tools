package com.damagephysics.physics;

public class Vec2 {
   public float x;
   public float y;

   public Vec2() {
      this.x = 0.0F;
      this.y = 0.0F;
   }

   public Vec2(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public Vec2(Vec2 other) {
      this.x = other.x;
      this.y = other.y;
   }

   public Vec2 set(float x, float y) {
      this.x = x;
      this.y = y;
      return this;
   }

   public Vec2 set(Vec2 other) {
      this.x = other.x;
      this.y = other.y;
      return this;
   }

   public Vec2 add(Vec2 other) {
      this.x = this.x + other.x;
      this.y = this.y + other.y;
      return this;
   }

   public Vec2 add(float dx, float dy) {
      this.x += dx;
      this.y += dy;
      return this;
   }

   public Vec2 sub(Vec2 other) {
      this.x = this.x - other.x;
      this.y = this.y - other.y;
      return this;
   }

   public Vec2 sub(float dx, float dy) {
      this.x -= dx;
      this.y -= dy;
      return this;
   }

   public Vec2 scale(float scalar) {
      this.x *= scalar;
      this.y *= scalar;
      return this;
   }

   public Vec2 scale(float sx, float sy) {
      this.x *= sx;
      this.y *= sy;
      return this;
   }

   public float dot(Vec2 other) {
      return this.x * other.x + this.y * other.y;
   }

   public float lengthSquared() {
      return this.x * this.x + this.y * this.y;
   }

   public float length() {
      return (float)Math.sqrt((double)(this.x * this.x + this.y * this.y));
   }

   public float distanceSquared(Vec2 other) {
      float dx = this.x - other.x;
      float dy = this.y - other.y;
      return dx * dx + dy * dy;
   }

   public float distance(Vec2 other) {
      return (float)Math.sqrt((double)this.distanceSquared(other));
   }

   public Vec2 normalize() {
      float lenSq = this.lengthSquared();
      if (lenSq > 1.0E-6F) {
         float invLen = 1.0F / (float)Math.sqrt((double)lenSq);
         this.x *= invLen;
         this.y *= invLen;
      } else {
         this.x = 0.0F;
         this.y = 0.0F;
      }

      return this;
   }

   public Vec2 copy() {
      return new Vec2(this.x, this.y);
   }

   public Vec2 setZero() {
      this.x = 0.0F;
      this.y = 0.0F;
      return this;
   }

   public static Vec2 lerp(Vec2 start, Vec2 end, float alpha, Vec2 out) {
      out.x = start.x + (end.x - start.x) * alpha;
      out.y = start.y + (end.y - start.y) * alpha;
      return out;
   }

   @Override
   public String toString() {
      return "Vec2(" + this.x + ", " + this.y + ")";
   }
}
