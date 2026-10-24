package com.damagephysics.physics;

public class DamageNumberContact {
   private static final float RESTITUTION = 0.25F;
   private static final float POSITION_CORRECTION_PERCENT = 0.4F;

   public static boolean resolve(DamageNumberBody a, DamageNumberBody b) {
      if (a.active && b.active) {
         float halfWidthA = a.width * 0.5F;
         float halfHeightA = a.height * 0.5F;
         float halfWidthB = b.width * 0.5F;
         float halfHeightB = b.height * 0.5F;
         float dx = b.position.x - a.position.x;
         float dy = b.position.y - a.position.y;
         float overlapX = halfWidthA + halfWidthB - Math.abs(dx);
         float overlapY = halfHeightA + halfHeightB - Math.abs(dy);
         if (!(overlapX <= 0.0F) && !(overlapY <= 0.0F)) {
            float normalX = 0.0F;
            float normalY = 0.0F;
            float penetration;
            if (overlapX < overlapY * 1.2F) {
               penetration = overlapX;
               normalX = dx < 0.0F ? -1.0F : 1.0F;
               if (dx == 0.0F) {
                  normalX = a.hashCode() > b.hashCode() ? 1.0F : -1.0F;
               }
            } else {
               penetration = overlapY;
               normalY = dy < 0.0F ? -1.0F : 1.0F;
               if (dy == 0.0F) {
                  normalY = a.hashCode() > b.hashCode() ? 1.0F : -1.0F;
               }
            }

            float totalInvMass = a.invMass + b.invMass;
            if (totalInvMass <= 1.0E-6F) {
               return false;
            } else {
               float correctionMagnitude = penetration * 0.4F / totalInvMass;
               a.position.x = a.position.x - normalX * correctionMagnitude * a.invMass;
               a.position.y = a.position.y - normalY * correctionMagnitude * a.invMass;
               b.position.x = b.position.x + normalX * correctionMagnitude * b.invMass;
               b.position.y = b.position.y + normalY * correctionMagnitude * b.invMass;
               float relVelX = b.velocity.x - a.velocity.x;
               float relVelY = b.velocity.y - a.velocity.y;
               float velAlongNormal = relVelX * normalX + relVelY * normalY;
               if (velAlongNormal > 0.0F) {
                  return true;
               } else {
                  float impulseMagnitude = -1.25F * velAlongNormal / totalInvMass;
                  float impulseX = normalX * impulseMagnitude;
                  float impulseY = normalY * impulseMagnitude;
                  a.velocity.x = a.velocity.x - impulseX * a.invMass;
                  a.velocity.y = a.velocity.y - impulseY * a.invMass;
                  b.velocity.x = b.velocity.x + impulseX * b.invMass;
                  b.velocity.y = b.velocity.y + impulseY * b.invMass;
                  a.angularVelocity -= impulseX * 0.05F;
                  b.angularVelocity += impulseX * 0.05F;
                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
