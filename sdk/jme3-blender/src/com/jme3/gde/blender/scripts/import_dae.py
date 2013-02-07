# This script invokes blender to import and save external model formats as
# .blend files to be processed further.
#
# Example usage for this importer:
#  blender --background --factory-startup --python $HOME/import_3ds.py -- \
#          --i="/tmp/hello.3ds" \
#          --o="/tmp/hello.blend" \
#
# See blender --help for details.

import bpy

# Imports a file using importer
def import_file(file_path):
    # Import the model
    bpy.ops.wm.collada_import(filepath = file_path)

# Clear existing objects.
def clear_scene():
    scene = bpy.context.scene
    scene.camera = None
    for obj in scene.objects:
        scene.objects.unlink(obj)

# Save current scene as .blend file
def save_file(save_path):
    # Check if output file exists already
    try:
        f = open(save_path, 'w')
        f.close()
        ok = True
    except:
        print("Cannot save to path %r" % save_path)

        import traceback
        traceback.print_exc()

    # Save .blend file
    if ok:
        bpy.ops.wm.save_as_mainfile(filepath=save_path)

# Due to an issue in the DAE importer, the file has to be resaved after an import
def open_save_file(save_path):
     bpy.ops.wm.open_mainfile(filepath=save_path)
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

    # Run the conversion
    clear_scene()
    import_file(args.file_path)
    save_file(args.save_path)
    open_save_file(args.save_path)

    print("batch job finished, exiting")


if __name__ == "__main__":
    main()
