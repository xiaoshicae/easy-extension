import { Highlight, Prism, themes, PrismTheme } from 'prism-react-renderer';
import React from 'react';

(typeof global !== 'undefined' ? global : window).Prism = Prism;
require('prismjs/components/prism-java');

/**
 * Extended VS Dark theme with Java-specific token styles.
 * prism-react-renderer's built-in vsDark doesn't cover
 * annotation, builtin, generics, etc.
 */
const javaTheme: PrismTheme = {
  ...themes.vsDark,
  styles: [
    ...themes.vsDark.styles,
    // Java annotations: @ExtensionPoint, @Override, etc.
    { types: ['annotation'], style: { color: '#DCDCAA' } },
    { types: ['builtin'], style: { color: '#DCDCAA' } },
    // Namespace (package, import paths)
    { types: ['namespace'], style: { color: '#B8D7A3' } },
    // Type parameters / generics
    { types: ['generics', 'type-variable'], style: { color: '#4EC9B0' } },
    // Doc tags: @param, @return
    { types: ['doc-comment', 'prolog'], style: { color: '#6A9955' } },
  ],
};

export type CodeHighlightProps = {
  code: string;
  language: string;
};

const CodeHighlight: React.FC<CodeHighlightProps> = ({ code, language }) => {
  return (
    <Highlight theme={javaTheme} code={code} language={language}>
      {({ className, style, tokens, getLineProps, getTokenProps }) => (
        <pre className={className} style={{ ...style, padding: '16px', borderRadius: '8px', fontSize: '13px', lineHeight: '1.6', overflow: 'auto' }}>
          {tokens.map((line, i) => (
            <div key={i} {...getLineProps({ line })}>
              <span style={{ display: 'inline-block', width: '2em', marginRight: '1em', textAlign: 'right', opacity: 0.4, userSelect: 'none' }}>{i + 1}</span>
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
