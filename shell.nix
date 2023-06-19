with (import <nixpkgs> {});

mkShell {
  buildInputs = [
    jdk17_headless
    sbt
    wkhtmltopdf-bin
    pngquant
    wget
 ];
  shellHook = ''
    export ARTTEK_HOME=`pwd`
  '';
}
