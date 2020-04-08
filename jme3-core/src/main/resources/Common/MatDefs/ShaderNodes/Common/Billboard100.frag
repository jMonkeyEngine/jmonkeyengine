void main(){
  // First column.
  worldViewMatrix[0][0] = scale; 
  worldViewMatrix[0][1] = 0.0; 
  worldViewMatrix[0][2] = 0.0; 
  
  // Second column.
  worldViewMatrix[1][0] = 0.0; 
  worldViewMatrix[1][1] = scale; 
  worldViewMatrix[1][2] = 0.0; 
  
  // Third column.
  worldViewMatrix[2][0] = 0.0; 
  worldViewMatrix[2][1] = 0.0; 
  worldViewMatrix[2][2] = scale;  

  vec4 position = worldViewMatrix * vec4(modelPosition,1.0);
  projPosition = projectionMatrix * position;
}
