package com.jme3.particles;

public enum BillboardMode {
  /**
   * Facing direction follows the velocity as it changes
   */
  Velocity,
  /**
   * Facing direction follows the velocity as it changes, Y of particle always
   * faces Z of velocity
   */
  Velocity_Z_Up,
  /**
   * Facing direction remains constant to the face of the particle particles
   * shape that the particle was emitted from
   */
  Normal,
  /**
   * Facing direction remains constant for X, Z axis' to the face of the
   * particle particles shape that the particle was emitted from. Y axis maps to
   * UNIT_Y
   */
  Normal_Y_Up,
  /**
   * ParticleData always faces camera
   */
  Camera,
  /**
   * ParticleData always faces X axis
   */
  UNIT_X,
  /**
   * ParticleData always faces Y axis
   */
  UNIT_Y,
  /**
   * ParticleData always faces Z axis
   */
  UNIT_Z,
  /**
   * ParticleData IS LEANED IN TOWARDS THE FRONT
   */
  UNIT_FORWARD
}
