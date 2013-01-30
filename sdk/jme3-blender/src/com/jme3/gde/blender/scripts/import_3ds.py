# This script is an example of how you can run blender from the command line
# (in background mode with no interface) to automate tasks, in this example it
# creates a text object, camera and light, then renders and/or saves it.
# This example also shows how you can parse command line options to scripts.
#
# Example usage for this test.
#  blender --background --factory-startup --python $HOME/background_job.py -- \
#          --text="Hello World" \
#          --render="/tmp/hello" \
#          --save="/tmp/hello.blend"
#
# Notice:
# '--factory-startup' is used to avoid the user default settings from
#                     interfearing with automated scene generation.
#
# '--' causes blender to ignore all following arguments so python can use them.
#
# See blender --help for details.

import bpy


def convert_file(file_path, save_path):
    bpy.ops.import_scene.autodesk_3ds(filepath = file_path)

    scene = bpy.context.scene

    try:
        f = open(save_path, 'w')
        f.close()
        ok = True
    except:
        print("Cannot save to path %r" % save_path)

        import traceback
        traceback.print_exc()

    if ok:
        bpy.ops.wm.save_as_mainfile(filepath=save_path)

def main():
    import sys       # to get command line args
    import argparse  # to parse options for us and print a nice help message

    # get the args passed to blender after "--", all of which are ignored by
    # blender so scripts may receive their own arguments
    argv = sys.argv

    if "--" not in argv:
        argv = []  # as if no args are passed
    else:
        argv = argv[argv.index("--") + 1:]  # get all args after "--"

    # When --help or no args are given, print this help
    usage_text = \
    "Run blender in background mode with this script:"
    "  blender --background --factory-startup --python " + __file__ + " -- [options]"

    parser = argparse.ArgumentParser(description=usage_text)

    # Possible types are: string, int, long, choice, float and complex.
    parser.add_argument("-i", "--input", dest="file_path", metavar='FILE',
            help="Import the specified file")
    parser.add_argument("-o", "--output", dest="save_path", metavar='FILE',
            help="Save the generated file to the specified path")

    args = parser.parse_args(argv)  # In this example we wont use the args

    if not argv:
        parser.print_help()
        return

    # Clear existing objects.
    scene = bpy.context.scene
    scene.camera = None
    for obj in scene.objects:
        scene.objects.unlink(obj)

    # Run the conversion
    convert_file(args.file_path, args.save_path)

    print("batch job finished, exiting")


if __name__ == "__main__":
    main()
