with (import <nixpkgs> {});

mkShell {
  buildInputs = [
    jdk17_headless
    sbt
    wkhtmltopdf-bin
  ];
  shellHook = ''
    export ARTTEK_HOME=`pwd`
  '';
}
