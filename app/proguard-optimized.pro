# The "optimized" build type exists for speed (R8 inlining/optimization, no debuggable
# runtime), not for size or secrecy: obfuscation buys nothing here and is the main
# source of reflection breakage, so it's off — which also keeps crash traces readable.
-dontobfuscate
