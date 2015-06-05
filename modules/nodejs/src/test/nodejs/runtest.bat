::
:: Copyright (C) GridGain Systems. All Rights Reserved.
::
:: _________        _____ __________________        _____
:: __  ____/___________(_)______  /__  ____/______ ____(_)_______
:: _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
:: / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
:: \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
::
::

::
:: Internal script that starts test runner.
::
if "%IGNITE_HOME%" == "" echo %0, ERROR: IGNITE_HOME environment vairable is not found.

nodeunit %IGNITE_HOME%\modules\nodejs\src\test\nodejs\test.js