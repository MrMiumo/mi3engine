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
     * @param x the x attribute of the other vector
     * @param y the y attribute of the other vector
     * @param z the z attribute of the other vector
     * @return the new vector resulting of the sum
     */
    public Vec add(double x, double y, double z) {
        return new Vec(this.x + x, this.y + y, this.z + z);
    }

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
     * Multiply this vector by the given one
     * @param other the other vector to multiply to this one
     * @return the new vector resulting of the multiplication
     */
    public Vec mul(Vec other) {
        return new Vec(x * other.x, y * other.y, z * other.z);
    }

    /** 
     * Divide this vector by the given value
     * @param divisor the value to divide to this one
     * @return the new vector resulting of the division
     */
    public Vec div(double divisor) {
        return new Vec(x / divisor, y / divisor, z / divisor);
    }

    /** 
     * Multiply this vector by the given value
     * @param other the value to multiply to this one
     * @return the new vector resulting of the multiplication
     */
    public Vec mul(double factor) {
        return new Vec(x * factor, y * factor, z * factor);
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
        // X
        double xx = cosY * cosZ;
        double xy = -cosY * sinZ;
        double xz = sinY;
        // Y
        double yx = sinX * sinY * cosZ + cosX * sinZ;
        double yy = -sinX * sinY * sinZ + cosX * cosZ;
        double yz = -sinX * cosY;
        // Z
        double zx = -cosX * sinY * cosZ + sinX * sinZ;
        double zy = cosX * sinY * sinZ + sinX * cosZ;
        double zz = cosX * cosY;

        return new Vec(
            xx * x + xy * y + xz * z,
            yx * x + yy * y + yz * z,
            zx * x + zy * y + zz * z
        );
    }

    /**
     * Converts an intrinsic rotation (on local axis) to a extrinsic
     * one (global axis) that can be given to the local 3D engine.
     * @return the corresponding global rotation of this vector (assumed
     *     as an intrinsic one)
     */
    public Vec localToGlobal() {
        double x = Math.toRadians(this.x);
        double y = Math.toRadians(this.y);
        double z = Math.toRadians(this.z);
        final double cosX = Math.cos(x), sinX = Math.sin(x);
        final double cosY = Math.cos(y), sinY = Math.sin(y);
        final double cosZ = Math.cos(z), sinZ = Math.sin(z);

        var m20 = sinX * sinZ + cosX * -sinY * cosZ;
        if (Math.abs(m20) < 0.999999) {
            var m00 = cosY * cosZ;
            var m10 = cosX * sinZ - sinX * -sinY * cosZ;
            var m21 = sinX * cosZ + cosX * -sinY * -sinZ;
            var m22 = cosX * cosY;
            return new Vec(
                Math.toDegrees(Math.atan2(m21, m22)),
                Math.toDegrees(Math.asin(-m20)),
                Math.toDegrees(Math.atan2(m10, m00))
            );
        } else {
            var m01 = cosY * -sinZ;
            var m11 = cosX * cosZ - sinX * -sinY * -sinZ;
            return new Vec(
                Math.toDegrees(0),
                Math.toDegrees(Math.asin(-m20)),
                Math.toDegrees(Math.atan2(-m01, m11))
            );
        }
    }

    @Override
    public String toString() {
        return String.format("Vec[%7.3f,%7.3f,%7.3f]", x, y, z);
    }
}