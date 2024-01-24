import argparse
import os
import shutil
from Python.utils.files_dirs_utils import has_trailing_spaces, remove_and_rename_trailing_spaces_dir

print("\n\n[INFO]: Build guide/categories DB started")

ap = argparse.ArgumentParser()
# add optional argument to select the path for the guides
ap.add_argument('-g', '--guides', required=True, help='path of guides')
# parse args# parse args
guide_name = ap.parse_args().guides

if guide_name:
    print('[INFO]: Using the guides for: ' + guide_name)
    pathGuides = "models/src/main/assets/guides/" + guide_name
    pathCategories = "models/src/main/assets/categories/" + guide_name
    # Check if the guide path exists
    if not os.path.exists(pathGuides):
        print("\n[ERROR]: The guides path " + pathGuides + " does not exist")
        exit(1)
    # Check if the categories path exists
    if not os.path.exists(pathCategories):
        print("\n[ERROR]: The categories path " + pathCategories + " does not exist")
        exit(1)

    print("\n[INFO]: Using the guides in " + pathGuides)
    # Delete old guides
    if os.path.exists("models/src/main/assets/currentGuide"):
        print("[INFO]: Deleting old guides")
        shutil.rmtree("models/src/main/assets/currentGuide")
    # Create new guides folder
    os.makedirs("models/src/main/assets/currentGuide")
    # Copy all the files from the provided path in models/src/main/assets/currentGuide
    print("[INFO]: Copying guides from " + pathGuides + " in " + os.path.realpath('models/src/main/assets/currentGuide'))
    dirs = os.listdir(pathGuides)
    print("[INFO]: Found " + str(len(dirs)) + " guides")
    for d in dirs:
        if has_trailing_spaces(d):
            print("[WARN]: Removing trailing spaces from '" + d + "'")
            source_dir = pathGuides + "/" + d
            os.rename(source_dir, source_dir.rstrip())
            d = d.rstrip()
        shutil.copytree(pathGuides + "/" + d, "models/src/main/assets/currentGuide/" + d)
    print("\n[INFO]: Using the categories in " + pathCategories)
    # Delete old categories
    if os.path.exists("models/src/main/assets/currentCategories"):
        print("[INFO]: Deleting old categories")
        shutil.rmtree("models/src/main/assets/currentCategories")
    # Create new categories folder
    # os.makedirs("models/src/main/assets/currentCategories")  # XXX seems there's no need to create it as the copy will do it
    # Copy all the files from pathCategories in models/src/main/assets/currentCategories
    print("[INFO]: Copying categories from " + pathCategories + " in " + os.path.realpath('models/src/main/assets/currentCategories'))
    shutil.copytree(pathCategories + "/", "models/src/main/assets/currentCategories/")
    print("\n[INFO]: Guides copied in " + os.path.realpath('models/src/main/assets/currentGuide'))
    print("[INFO]: Categories copied in " + os.path.realpath('models/src/main/assets/currentCategories'))
else:
    print("\n[ERROR]: You must specify the path of the guides with the argument -g")
    exit(1)

print("\n\n[INFO]: Copy guide/categories terminated correctly")
exit(0)
