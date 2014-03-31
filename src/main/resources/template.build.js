(function (appDir, baseUrl, dir, paths, buildWriter) {
    return {
        appDir: appDir,
        baseUrl: baseUrl,
        dir: dir,
        generateSourceMaps: true,
        mainConfigFile: appDir + "/" + baseUrl + "/main.js",
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
}(undefined, undefined, undefined, undefined, undefined))
