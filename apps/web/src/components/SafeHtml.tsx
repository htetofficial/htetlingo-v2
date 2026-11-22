import { useEffect, useRef } from 'react';
import { safeSetHTML } from '../lib/sanitize.ts';

interface SafeHtmlProps {
  html: string;
  className?: string;
}

export function SafeHtml({ html, className }: SafeHtmlProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    const node = ref.current;
    if (!node) return;
    safeSetHTML(node, html).catch(() => {
      if (active && ref.current) {
        ref.current.textContent = html.replace(/<[^>]+>/g, ' ');
      }
    });
    return () => {
      active = false;
    };
  }, [html]);

  return <div ref={ref} className={className} />;
}
