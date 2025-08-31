package io.github.mrmiumo.mi3engine;

/**
 * Representation of a three-dimensional vector in a XYZ base
 * @param x the size of the vector on the X axis
 * @param y the size of the vector on the Y axis
 * @param z the size of the vector on the Z axis
 */
public record Vec(double x, double y, double z) {

    /** The zero vector with all axis on 0 */
    public static final Vec ZERO = new Vec(0, 0, 0);

    /**
     * Computes the sum of this vector with the given one.
     * @param other the other vector to sum with this one
     * @return the new vector resulting of the sum
     */
    public Vec add(Vec other) {
        return new Vec(x + other.x, y + other.y, z + other.z);
    }

    /**
     * Subtracts the given vector to this one
     * @param other the other vector to subtract with this one
     * @return the new vector resulting of the sum
     */
    public Vec sub(Vec other) {
        return new Vec(x - other.x, y - other.y, z - other.z);
    }
    
    /**
     * Calculates the cross vector between this ont and the given one.
     * @param other the vector to cross with
     * @return the cross vector
     */
    public Vec cross(Vec other) {
        double newX = this.y * other.z - this.z * other.y;
        double newY = this.z * other.x - this.x * other.z;
        double newZ = this.x * other.y - this.y * other.x;
        return new Vec(newX, newY, newZ);
    }

    /**
     * Multiply each component of this vector with the other one and
     * sum them up.
     * @param other the other vector to multiply with
     * @return the sum of the multiplications
     */
    public double dot(Vec other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /**
     * Normalizes this vector (change its components by keeping
     * proportions) to set its length to 1.
     * @return the normalized vector
     */
    public Vec normalize() {
        double l = length();
        if (l == 0) return new Vec(0, 0, 0);
        return new Vec(x / l, y / l, z / l);
    }

    /**
     * Computes the length of the vector after computing each component
     * @return the length of the segment represented by the vector
     */
    public double length() {
        return Math.sqrt(x*x + y*y + z*z);
    }

    /**
     * Rotates this vector by the given amount on the x, y and z axis.
     * @param deg the amount of rotation to apply on each axis in degrees
     * @return the rotated vector
     */
    public Vec rotate(Vec deg) {
        double pitch = Math.toRadians(deg.x());
        double yaw   = Math.toRadians(deg.y());
        double roll  = Math.toRadians(deg.z());

        double cosX = Math.cos(pitch);
        double sinX = Math.sin(pitch);
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double cosZ = Math.cos(roll);
        double sinZ = Math.sin(roll);

        // Y
        double x1 =  cosY * x +  sinY * z;
        double y1 =  y;
        double z1 = -sinY * x +  cosY * z;
        // X
        double x2 = x1;
        double y2 =  cosX * y1 - sinX * z1;
        double z2 =  sinX * y1 + cosX * z1;
        // Z
        double x3 =  cosZ * x2 - sinZ * y2;
        double y3 =  sinZ * x2 + cosZ * y2;
        double z3 =  z2;

        return new Vec(x3, y3, z3);
    }
}