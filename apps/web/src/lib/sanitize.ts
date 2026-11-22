let domPurifyPromise: Promise<typeof import('dompurify')['default']> | undefined;

function loadDOMPurify() {
  domPurifyPromise ??= import('dompurify').then((mod) => mod.default ?? (mod as unknown as typeof import('dompurify')['default']));
  return domPurifyPromise;
}

const ALLOWED_ELEMENTS = ['b', 'i', 'br', 'strong', 'em', 'p', 'span'];
const ALLOWED_ATTRIBUTES = ['class'];

type ElementWithSetHTML = HTMLElement & {
  setHTML?: (html: string, options?: { sanitizer?: { elements: string[]; attributes: string[] } }) => void;
};

/**
 * Safely renders dictionary HTML using native Element.setHTML() when available,
 * falling back lazily to DOMPurify configured to match native Sanitizer rules.
 */
export async function safeSetHTML(el: HTMLElement, html: string): Promise<void> {
  if ('setHTML' in Element.prototype && typeof (el as ElementWithSetHTML).setHTML === 'function') {
    (el as ElementWithSetHTML).setHTML!(html, {
      sanitizer: { elements: ALLOWED_ELEMENTS, attributes: ALLOWED_ATTRIBUTES },
    });
  } else {
    const DOMPurify = await loadDOMPurify();
    el.innerHTML = DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [...ALLOWED_ELEMENTS, '#text'],
      ALLOWED_ATTR: ALLOWED_ATTRIBUTES,
      ALLOW_ARIA_ATTR: false,
      ALLOW_DATA_ATTR: false,
      KEEP_CONTENT: false,
    });
  }
}
