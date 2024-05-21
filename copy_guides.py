import argparse
import os
import shutil
from Python.utils.files_dirs_utils import has_trailing_spaces, remove_and_rename_trailing_spaces_dir
#from Python.processing_monuments import createDB

print("\n\n[INFO]: Build guide/categories DB started")

ap = argparse.ArgumentParser()
# add optional argument to select the path for the guides
ap.add_argument('-g', '--guides', required=True, help='path of guides')
# parse args# parse args
guide_name = ap.parse_args().guides

if guide_name:
    print('[INFO]: Using the guides for: ' + guide_name)
    sourceGuides = "Python/guides/" + guide_name
    sourceCategories = "Python/categories/" + guide_name
    # Check if the guide path exists
    if not os.path.exists(sourceGuides):
        print("\n[ERROR]: The guides path " + sourceGuides + " does not exist")
        exit(1)
    # Check if the categories path exists
    if not os.path.exists(sourceCategories):
        print("\n[ERROR]: The categories path " + sourceCategories + " does not exist")
        exit(1)

    print("\n[INFO]: Using the guides in " + sourceGuides)
    # Delete old guides
    if os.path.exists("models/src/main/assets/currentGuide"):
        print("[INFO]: Deleting old guides")
        shutil.rmtree("models/src/main/assets/currentGuide")
    # Create new guides folder
    os.makedirs("models/src/main/assets/currentGuide")
    # Copy all the files from the provided path in models/src/main/assets/currentGuide
    print("[INFO]: Copying guides from " + sourceGuides + " in " + os.path.realpath('models/src/main/assets/currentGuide'))
    dirs = os.listdir(sourceGuides)
    print("[INFO]: Found " + str(len(dirs)) + " guides")
    for d in dirs:
        if has_trailing_spaces(d):
            print("[WARN]: Removing trailing spaces from '" + d + "'")
            source_dir = sourceGuides + "/" + d
            os.rename(source_dir, source_dir.rstrip())
            d = d.rstrip()
        # Check if .DS_Store file exists in the directory
        dir_path = sourceGuides + "/" + d
        if os.path.exists(os.path.join(dir_path, ".DS_Store")):
            # If it exists, remove it
            os.remove(os.path.join(dir_path, ".DS_Store"))
        shutil.copytree(dir_path, "models/src/main/assets/currentGuide/" + d)
    print("\n[INFO]: Using the categories in " + sourceCategories)
    # Delete old categories
    if os.path.exists("models/src/main/assets/currentCategories"):
        print("[INFO]: Deleting old categories")
        shutil.rmtree("models/src/main/assets/currentCategories")
    # Create new categories folder
    # os.makedirs("models/src/main/assets/currentCategories")  # XXX seems there's no need to create it as the copy will do it
    # Copy all the files from sourceCategories in models/src/main/assets/currentCategories
    print("[INFO]: Copying categories from " + sourceCategories + " in " + os.path.realpath('models/src/main/assets/currentCategories'))
    shutil.copytree(sourceCategories + "/", "models/src/main/assets/currentCategories/")
    print("\n[INFO]: Guides copied in " + os.path.realpath('models/src/main/assets/currentGuide'))
    print("[INFO]: Categories copied in " + os.path.realpath('models/src/main/assets/currentCategories'))

    #print("\n[INFO]: creating monuments DB in " + os.path.realpath('models/src/main/assets/databases/monuments_db.sqlite'))
    #createDB(True)  ## FIXME deal with imports... this script is not launched in the Docker, some imports may not be there
else:
    print("\n[ERROR]: You must specify the path of the guides with the argument -g")
    exit(1)

print("\n\n[INFO]: Copy guide/categories terminated correctly")
exit(0)
