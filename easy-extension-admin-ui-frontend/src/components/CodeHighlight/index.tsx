import { Highlight, Prism, themes } from 'prism-react-renderer';
import React from 'react';

(typeof global !== 'undefined' ? global : window).Prism = Prism;
require('prismjs/components/prism-java');

export type CodeHighlightProps = {
  code: string;
  language: string;
};

const CodeHighlight: React.FC<CodeHighlightProps> = ({ code, language }) => {
  return (
    <Highlight theme={themes.vsDark} code={code} language={language}>
      {({ className, style, tokens, getLineProps, getTokenProps }) => (
        <pre className={className} style={style}>
          {tokens.map((line, i) => (
            <div key={i} {...getLineProps({ line })}>
              {line.map((token, key) => (
                <span key={key} {...getTokenProps({ token })} />
              ))}
            </div>
          ))}
        </pre>
      )}
    </Highlight>
  );
};

export default CodeHighlight;
