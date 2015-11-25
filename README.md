**NOTE:** This project won't be further developed as it has been replaced by [ums-mlx](https://github.com/taconaut/ums-mlx).

# PS3 Media Server MLX

by shagrath

[![Build Status](http://ps3mediaserver-ci.hopto.org:9999/job/pms-mlx%20trunk/badge/icon)](http://ps3mediaserver-ci.hopto.org:9999/job/pms-mlx%20trunk)

- [Links](#links)
- [Thanks](#thanks)
- [Installation](#installation)
- [Building](#building)
- [Development](#development)
- [License](#license)

PS3 Media Server is a cross-platform DLNA-compliant UPnP Media Server.
Originally written to support the PlayStation 3, PS3 Media Server has been
expanded to support a range of other media renderers, including smartphones,
TVs, music players and more.

## Links

* [Website](http://www.ps3mediaserver.org/)
* [Forum](http://www.ps3mediaserver.org/forum/)
* [Downloads](http://sourceforge.net/projects/ps3mediaserver/files/)
* [Source code](https://github.com/ps3mediaserver/ps3mediaserver)
* [Issue tracker](https://code.google.com/p/ps3mediaserver/issues/list)

## Thanks

Thanks to:

* Redlum
* tcox
* SubJunk
* taconaut
* tomeko
* lightglitch
* chocolateboy
* ditlew
* Raptor399
* renszarv
* happy.neko

for major code contributions.

Thanks to:

* meskibob
* otmanix

for documentation and contributions to the community.

* boblinds and snoots for the network test cases :)
* sarraken, bleuecinephile, bd.azerty, fabounnet for the support and feedback
* smo for the Jenkins server

See the [CHANGELOG](https://github.com/ps3mediaserver/ps3mediaserver/blob/master/CHANGELOG.txt) for more thanks.

## Installation

The [download site](http://sourceforge.net/projects/ps3mediaserver/files/)
has the latest releases of PS3 Media Server for Windows and Mac OS X as well as tarballs for Linux/Unix
and debs for manual installation on Debian/Ubuntu.

For Debian and Ubuntu packages, see [here](http://www.ps3mediaserver.org/forum/viewtopic.php?f=3&t=13046).

For instructions on installing and running PMS from a tarball, see
[INSTALL.txt](https://github.com/ps3mediaserver/ps3mediaserver/blob/master/INSTALL.txt).

## Building

PMS can be built using the following commands:

    git clone git://github.com/ps3mediaserver/ps3mediaserver.git
    cd ps3mediaserver
    mvn com.savage7.maven.plugins:maven-external-dependency-plugin:resolve-external
    mvn com.savage7.maven.plugins:maven-external-dependency-plugin:install-external
    mvn package

See [BUILD.md](https://github.com/ps3mediaserver/ps3mediaserver/blob/master/BUILD.md) for detailed information
on setting up a PMS build environment.

## Development

If you plan to commit source code, be sure to configure git to deal properly with
cross platform line endings.

On Mac OS X and Linux:

    git config --global core.autocrlf input

On Windows:

    git config --global core.autocrlf true

For more information, see http://help.github.com/line-endings/

See [DEVELOP.md](https://github.com/ps3mediaserver/ps3mediaserver/blob/master/DEVELOP.md)
for detailed information on setting up a PMS development environment.

## License

Copyright 2009-2013 shagrath.

PS3 Media Server is free software: you can redistribute it and/or modify it under the terms of the
[GNU General Public License](https://github.com/ps3mediaserver/ps3mediaserver/blob/master/LICENSE.txt)
as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version.

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/taconaut/pms-mlx/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
