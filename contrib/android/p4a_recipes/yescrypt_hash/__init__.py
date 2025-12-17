from pythonforandroid.recipe import CythonRecipe


class YescryptHashRecipe(CythonRecipe):

    url = ('https://files.pythonhosted.org/packages/'
           'source/y/yescrypt_hash/yescrypt_hash-{version}.tar.gz')
    md5sum = 'da930575a18ebabb0baebb15dab0a19e'
    version = '0.5'
    depends = ['python3']


recipe = YescryptHashRecipe()