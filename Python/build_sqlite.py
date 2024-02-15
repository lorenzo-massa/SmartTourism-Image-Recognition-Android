# -*- coding: utf-8 -*-
import glob
import argparse
from preprocessors.aspectawarepreprocessor import printIMG
import progressbar
import numpy as np
from scripts import Extractor
from preprocessors import AspectAwarePreprocessor
from preprocessors import ImageToArrayPreprocessor
import cv2
import os
import shutil
import sqlite3
import pickle
from sklearn.model_selection import train_test_split
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
import glob
from processing_monuments import createDB
from utils.files_dirs_utils import has_trailing_spaces, remove_and_rename_trailing_spaces_dir
from deep_augumentation import augment

landmark_names = set()  # used to check if all guides have corresponding images

def check_guides_and_images(guides_list: set[str], monuments_images_list: set[str]) -> None:
    # Find elements in set1 but not in set2
    elements_only_in_guides_list = guides_list - monuments_images_list

    # Find elements in set2 but not in set1
    elements_only_in_monuments_images = monuments_images_list - guides_list

    # Print differences
    if elements_only_in_guides_list:
        print(f"[INFO] Elements of the guide without corresponding images: {elements_only_in_guides_list}")

    if elements_only_in_monuments_images:
        print(f"[WARN] Elements only in monuments' images without corresponding guides: {elements_only_in_monuments_images}")

    if not elements_only_in_guides_list and not elements_only_in_monuments_images:
        print("[INFO] Guides and monuments' images lists are consistent")


np.set_printoptions(threshold=np.inf)

types = [  # neural networks
    ('MobileNetV3_Large_100', 'models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_100_224_classification_5_default_1.tflite'),
    ('MobileNetV3_Large_075', 'models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_075_224_classification_5_default_1.tflite'),
    ('MobileNetV3_Small_100', 'models/src/main/assets/lite-model_imagenet_mobilenet_v3_small_100_224_classification_5_default_1.tflite')
]

# Set ALL_DATASET to True if you want to use all the images of the dataset to train the model,
# set to False if you want to split the dataset in train_set and test_set
ALL_DATASET = True

print("\n\n[INFO]: Build guide DB started")

ap = argparse.ArgumentParser()
# add optional argument to select the path for the guides
ap.add_argument('-g', '--guides', help='path of guides')
# add optional argument to skip the creation of the test_set (default False)
ap.add_argument('-f', '--fast', help='skip the creation of the features dataset', action='store_true')
# add option argument verbose (dafault False)
ap.add_argument('-v', '--verbose', help='verbose', action='store_true')
verbose = ap.parse_args().verbose
if verbose:
    print("[INFO]: Verbose mode enabled")
else:
    widgets = ["[INFO]: Building dataset and processing monuments ...",
            progressbar.Percentage(), " ", progressbar.Bar(), " ", progressbar.ETA()]
    pbar = progressbar.ProgressBar(maxval=100, widgets=widgets).start()

# parse args
guide_name = ap.parse_args().guides

if guide_name:
    if verbose:
        print('[INFO]: Using the guides for ' + guide_name)
    pathGuides = "Python/guides/" + guide_name
    pathCategories = "Python/categories/" + guide_name
    pathImages = "Python/imageDatasets/" + guide_name
    if verbose:
        print("\n[INFO]: Using the guides in " + pathGuides)
    # Delete old guides
    if os.path.exists("models/src/main/assets/currentGuide"):
        if verbose:
            print("[INFO]: Deleting old guides")
        shutil.rmtree("models/src/main/assets/currentGuide")
    # Create new guides folder
    os.makedirs("models/src/main/assets/currentGuide")
    # Copy all the files from the provided path in models/src/main/assets/currentGuide
    if verbose:
        print("[INFO]: Copying guides from " + pathGuides + " in " + os.path.realpath('models/src/main/assets/currentGuide'))
    dirs = os.listdir(pathGuides)
    if verbose:
        print("[INFO]: Found " + str(len(dirs)) + " guides")
    for d in dirs:
        if has_trailing_spaces(d):
            print("[WARN]: Removing trailing spaces from '" + d + "'")
            source_dir = pathGuides + "/" + d
            os.rename(source_dir, source_dir.rstrip())
            d = d.rstrip()
        shutil.copytree(pathGuides + "/" + d, "models/src/main/assets/currentGuide/" + d)
        landmark_names.add(d)
    if verbose:
        print("\n[INFO]: Guides copied in " + os.path.realpath('models/src/main/assets/currentGuide'))
        print("\n[INFO]: Using the categories in " + pathCategories)
    # Delete old categories
    if os.path.exists("models/src/main/assets/currentCategories"):
        if verbose:
            print("[INFO]: Deleting old categories")
        shutil.rmtree("models/src/main/assets/currentCategories")
    # Create new categories folder
    # os.makedirs("models/src/main/assets/currentCategories")  # XXX seems there's no need to create it as the copy will do it
    # Copy all the files from pathCategories in models/src/main/assets/currentCategories
    if verbose:
        print("[INFO]: Copying categories from " + pathCategories + " in " + os.path.realpath('models/src/main/assets/currentCategories'))
    shutil.copytree(pathCategories + "/", "models/src/main/assets/currentCategories/")
    if verbose:
        print("[INFO]: Categories copied in " + os.path.realpath('models/src/main/assets/currentCategories'))
else:
    print("\n[ERROR]: You must specify the path of the guides with the argument -g")
    exit()

if ap.parse_args().fast:
    print("\n\n[INFO]: Skipping the creation of the features dataset")
    createDB(verbose)
    exit()

if verbose:
    print("\n\n[INFO]: Using the images in " + pathImages)
# eliminate trailing spaces in image directories to match the names fo the guide directories
[remove_and_rename_trailing_spaces_dir(path) for path in os.listdir(pathImages)]

# LOAD IMAGE PATHS
image_directories = [d for d in os.listdir(pathImages) if os.path.isdir(os.path.join(pathImages, d))]

dataImages = []
for directory in image_directories:
    image_paths_jpg = glob.glob(os.path.join(pathImages, directory, '*.jpg'))
    image_paths_jpeg = glob.glob(os.path.join(pathImages, directory, '*.jpeg'))
    image_paths_png = glob.glob(os.path.join(pathImages, directory, '*.png'))
    dataImages.extend(image_paths_jpg + image_paths_jpeg + image_paths_png)

dataset = list()

# GENERATING SETS
if not ALL_DATASET:
    train_set, test_set = train_test_split(
        dataImages, test_size=0.33, random_state=1331, shuffle=True
    )
    dataImages = train_set

# progress bar
if verbose:
    widgets = [
        "Building dataset ...", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(
        maxval=len(dataImages), widgets=widgets
    ).start()

if len(dataImages) == 0:
    print("\n\n[ERROR]: No images found in the specified path: " + pathImages + "\n\n")
    exit()

monuments = dict()

for (i, path) in enumerate(dataImages):
    if path.split(os.path.sep)[-2].count('_') > 0:
        pathSplitted = path.split(os.path.sep)[-2].split('_')
    else:
        pathSplitted = path.split(os.path.sep)[-2].split(' ')
    monument = ' '.join(pathSplitted)
    tupleImage = (monument, path)
    dataset.append(tupleImage)
    if monument not in monuments.keys():
        monuments[monument] = 1
    else:
        monuments[monument] += 1
    if verbose:
        pbar.update(i)
if verbose:
    pbar.finish()
else:
    pbar.update(10)

if verbose:
    check_guides_and_images(landmark_names, set(monuments.keys()))

IMAGE_PER_MONUMENT = 20

# for each monument with less than 20 images, augment the dataset with the images of the same monument calling de function augment_images
for monument in monuments.keys():
    if monuments[monument] < IMAGE_PER_MONUMENT:
        if verbose:
            print("\n\n[INFO]: Augmenting " + str(IMAGE_PER_MONUMENT - monuments[monument]) + " images of " + monument + " ..." )
        # Get n random images of the monument
        images = [path for (m, path) in dataset if m == monument and "_aug" not in path]
        images = np.random.choice(images, size=(IMAGE_PER_MONUMENT - monuments[monument]))
        i = 1
        for image in images:
            newpath = augment(image, i)
            tupleImage = (monument, newpath)
            dataset.append(tupleImage)
            i += 1

# BUILD FEATURES

iap = ImageToArrayPreprocessor()
aap = AspectAwarePreprocessor(224, 224)

if not verbose:
    pbar.update(15)
    actualPercentage = 15

# loop over images
for dType, modelPath in types:
    if verbose:
        print('[INFO]: Working with {} ...'.format(dType))
    extractor = Extractor(dType, path=modelPath)
    db = []
    if verbose:
        widgets = [
            "Extracting features ... ", progressbar.Percentage(), " ",
            progressbar.Bar(), " ", progressbar.ETA()
        ]
        pbar = progressbar.ProgressBar(maxval=len(dataset), widgets=widgets).start()

    index = 0

    for monument, path in dataset:
        # preprocessing
        image = cv2.imread(path)
        if np.shape(image) == ():  # latest numpy / py3
            print("\n\n[ERROR]: Image not found: " + str(path) + " for landmark: " + str(monument))
            continue  # fail !!
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        h = image.shape[0]
        w = image.shape[1]

        # AUGUMENTATION 3X3

        imgheight = h - (h % 3)
        imgwidth = w - (w % 3)

        y1 = 0
        M = imgheight // 3
        N = imgwidth // 3

        tiles = list()

        for y in range(0, imgheight, M):
            for x in range(0, imgwidth, N):
                y1 = y + M
                x1 = x + N
                tiles.append(image[y:y + M, x:x + N])

        # PREPROCESS IMG WITH AUGUMENTATION ZOOM

        image, image1, image2 = aap.preprocessAugumentation(image, 0.5, 0.3)
        # toArray preprocess
        image = iap.preprocess(image)
        image1 = iap.preprocess(image1)
        image2 = iap.preprocess(image2)

        features = extractor.extract(image)
        features1 = extractor.extract(image1)
        features2 = extractor.extract(image2)

        # SAVING
        if isinstance(features, np.ndarray):
            db.append([features, monument])
        if isinstance(features1, np.ndarray):
            db.append([features1, monument])
        if isinstance(features2, np.ndarray):
            db.append([features2, monument])

        # PREPROCESS TILES FOR AUGUMENTATION 3x3

        for t in tiles:
            t = aap.preprocess(t)

            # toArray preprocess
            t = iap.preprocess(t)
            featuresTile = extractor.extract(t)

            # SAVING
            if isinstance(featuresTile, np.ndarray):
                db.append([featuresTile, monument])

        index += 1
        if verbose:
            pbar.update(index)
    if verbose:
        pbar.finish()
    else:
        actualPercentage += 15
        pbar.update(actualPercentage)


    # CREATING NEW SQL LITE DATABASE FOR VISUAL FEATURES

    # if databases folder does not exist, create it
    if not os.path.exists("models/src/main/assets/databases"):
        os.makedirs("models/src/main/assets/databases")

    # Delete old database
    if os.path.exists("models/src/main/assets/databases/" + dType + "_db.sqlite"):
        os.remove("models/src/main/assets/databases/" + dType + "_db.sqlite")

    # Create new database
    con = sqlite3.connect("models/src/main/assets/databases/" + dType + "_db.sqlite")
    cur = con.cursor()

    cur.execute("DROP TABLE IF EXISTS monuments")
    cur.execute(""" CREATE TABLE monuments (monument, features) """)

    if verbose:
        widgets = [
            "Saving database ... ", progressbar.Percentage(), " ",
            progressbar.Bar(), " ", progressbar.ETA()
        ]
        pbar = progressbar.ProgressBar(maxval=len(db), widgets=widgets).start()

    for i, (matrix, m) in enumerate(db):
        # Insert a row of data
        val = str(matrix)

        sql = ''' INSERT INTO monuments (monument,features)
                VALUES(?,?) '''
        new = cur.execute(sql, (m, val))

        # Save (commit) the changes
        con.commit()
        if verbose:
            pbar.update(i)

    con.close()
    if verbose:
        pbar.finish()
    else:
        actualPercentage += 10
        pbar.update(actualPercentage)

# print("\n\nDB Saved in " + os.path.realpath('../models/src/main/assets/databases'))

if verbose:
    print("\n\n[INFO]: Processing monuments")
else:
    pbar.update(90)

createDB(verbose)

if not verbose:
    pbar.finish()

print("\n\n[INFO]: Build DB script terminated correctly")
