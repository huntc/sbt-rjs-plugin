/*
 * This is a build writing function for RequireJS. The purpose of it is to substitute any string literals that
 * have a value as per libPath. The libPath is used to determine the name of a module that can be found in paths.
 * For example if libPath is "lib/" and paths is {"underscorejs":"http://somecdn/underscorejs"} then any string literals
 * containing "lib/underscorejs/underscore" will be substituted with "http://somecdn/underscorejs/underscore".
 *
 * The function is intended to perform well given the initial scanning phase. Once the substitution array is collected
 * then it is looped over and string concatenations are performed. The common scenario of a file not having anything
 * to substitute results in only one concatenation. There are only (n * 2) + 1 concatenations to perform where n
 * is the number of substitutions.
 */
(function (libPath, paths, moduleIds) {
    function advance(contents, s, i) {
        i = contents.indexOf(s, i);
        if (i === -1) {
            i = contents.length;
        } else {
            i += s.length - 1;
        }
        return i;
    }

    return function () {
        var contents = arguments[2];
        var substitutions = [];

        var prevC = "";
        for (var i = 0; i < contents.length; ++i) {
            var artifactId, j, k, l, m, moduleId, modulePath, pathChange;

            var c = contents[i];
            if (c === "*" && prevC === "/") {
                i = advance(contents, "*/", i);
                prevC = "";
            } else if (c === "/" && prevC === "/") {
                i = advance(contents, "\n", i);
                prevC = "";
            } else if (c === '"' || c === "'") {
                j = advance(contents, c, i + 1);
                k = advance(contents, libPath, i) + 1;
                if (k < j) {
                    l = contents.indexOf("/", k);
                    if (l === -1 || l > j) {
                        l = j;
                    }
                    artifactId = contents.substring(k, l);
                    pathChange = paths[artifactId];
                    if (pathChange !== undefined) {
                        modulePath = contents.substring(l, j);

                        m = modulePath.lastIndexOf("/");
                        moduleId = modulePath.substring(m + 1);
                        if (moduleIds[moduleId + ".min"] !== undefined) {
                            modulePath += ".min";
                        } else if (moduleIds[moduleId + "-min"] !== undefined) {
                            modulePath += "-min";
                        }
                        pathChange += modulePath;

                        substitutions.push({
                            start: i,
                            end: j,
                            change: pathChange
                        });
                    }
                }
                prevC = "";
                i = j;
            } else {
                prevC = c;
            }
        }

        var newContents = "";
        var prevEnd = 0;
        substitutions.forEach(function (substitution) {
            newContents += contents.substring(prevEnd, substitution.start + 1);
            newContents += substitution.change;
            prevEnd = substitution.end;
        });
        newContents += contents.substring(prevEnd);

        return newContents;
    };

})(undefined, undefined, undefined);