import glob
import hashlib
import os

import numpy as np
from cv2 import cv2
from tqdm import tqdm

path = "../common/src/main/resources/assets/mca/skins/clothing/"

# we allow 10 seconds to check out to avoid considering clothes outdated because of a checkout
checkout_time = 10


def img_load(img_file):
    return cv2.imread(img_file, flags=cv2.IMREAD_UNCHANGED).astype(float) / 255.0


# masks and textures
torn_tex = img_load("res/torn.png")
moss_tex = img_load("res/moss.png")


def convert_to_zombie(skin_file: str):
    img = img_load(skin_file)

    # to make results consistent across runs
    h = hashlib.md5(bytes(skin_file, "ascii")).digest()
    r = h[0] / 256 ** 4 + h[1] / 256 ** 3 + h[2] / 256 ** 2 + h[3] / 256 ** 1

    # torn effect
    result = img.copy()
    result[:, :, 0:3] *= (0.25 + torn_tex[:, :, 0:3] * 1.125)
    result[:, :, 3] *= torn_tex[:, :, 3]

    # moss effect
    moss_tex_rolled = np.roll(moss_tex, int(r * 64), axis=0)
    moss_tex_rolled = np.roll(moss_tex_rolled, int(r * 64), axis=1)
    alpha = np.stack((moss_tex_rolled[:, :, 3] * img[:, :, 3],) * 3, axis=2)
    result[:, :, 0:3] *= (1.0 - alpha)
    result[:, :, 0:3] += moss_tex_rolled[:, :, 0:3] * alpha

    return (result * 255.0).astype(np.uint8)


if __name__ == "__main__":
    files_source = glob.glob(os.path.join(path, "normal/*/*/*.png"))
    files_zombie = glob.glob(os.path.join(path, "zombie/*/*/*.png"))

    files_zombie_set = set(files_zombie)

    for file in tqdm(files_source):
        # check if zombie clothes either does not exist or is outdated
        zombie_file = file.replace("normal", "zombie")
        if zombie_file not in files_zombie_set or os.path.getmtime(file) > os.path.getmtime(
                zombie_file) + checkout_time:
            # convert
            img = convert_to_zombie(file)

            # also make sure to avoid duplicates
            if zombie_file in files_zombie:
                original = img_load(zombie_file)
                if (original - img).max() > 2:
                    continue

            # writes
            os.makedirs(os.path.dirname(zombie_file), exist_ok=True)
            cv2.imwrite(zombie_file, img)
