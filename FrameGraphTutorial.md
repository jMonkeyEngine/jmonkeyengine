
/**
 * For this tutorial, we will create a simple FrameGraph that renders
 * the opaque, sky, and transparent buckets in that order, followed
 * by a pass that renders the result to the screen.
 */

// Begin by declaring a new FrameGraph.
FrameGraph fg = new FrameGraph();

// Add the passes we will use, in the correct order.
// Make sure to specify the correct bucket on each pass.
BucketPass opaque = fg.add(new BucketPass(Bucket.Opaque));
BucketPass sky = fg.add(new BucketPass(Bucket.Sky));
BucketPass transparent = fg.add(new BucketPass(Bucket.Transparent));
OutputPass output = fg.add(new OutputPass());

// Now, each pass needs to pass its resulting color and depth to the next pass.
// We will do that by connecting certain tickets belonging to the passes.
sky.makeInput(opaque, "Color", "Color");
sky.makeInput(opaque, "Depth", "Depth");
transparent.makeInput(sky, "Color", "Color");
transparent.makeInput(sky, "Depth", "Depth");
output.makeInput(transparent, "Color", "Color");
output.makeInput(transparent, "Depth", "Depth");

// The graph now, visually, looks like this:
//
//  Opaque:   Sky:      Transparent:    Output:
//  color  -> color  -> color  -------> color
//  depth  -> depth  -> depth  -------> depth

// Finally, assign the FrameGraph to the ViewPort.
viewPort.setFrameGraph(fg);


