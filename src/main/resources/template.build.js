(function (appDir, dir, paths, buildWriter) {
    return {
        appDir: appDir,
        baseUrl: "js",
        dir: dir,
        generateSourceMaps: true,
        mainConfigFile: appDir + "/js/main.js",
        modules: [
            {
                name: "main"
            }
        ],
        onBuildWrite: buildWriter,
        optimize: "uglify2",
        paths: paths,
        preserveLicenseComments: false
    }
}(undefined, undefined, undefined, undefined))
