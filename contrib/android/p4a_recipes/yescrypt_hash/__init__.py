from pythonforandroid.recipe import CythonRecipe


class YescryptHashRecipe(CythonRecipe):

    url = ('https://files.pythonhosted.org/packages/'
           'source/y/yescrypt_hash/yescrypt_hash-{version}.tar.gz')
    md5sum = '94b03a79e23cca91b96601280505ec96'
    version = '0.5.1'
    depends = ['python3']


recipe = YescryptHashRecipe()