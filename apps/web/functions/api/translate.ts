// Cloudflare Pages Function: /api/translate
// Free-plan serverless proxy for multilingual translation with clean error handling

interface TranslateRequest {
  text: string;
  source: string;
  target: string;
}

export const onRequestPost = async (context: { request: Request }) => {
  try {
    const body = (await context.request.json()) as TranslateRequest;
    const text = (body.text ?? '').trim();
    const source = (body.source ?? 'en').slice(0, 5);
    const target = (body.target ?? 'my').slice(0, 5);

    if (!text) {
      return Response.json({ error: 'Text is required' }, { status: 400 });
    }
    if (text.length > 1000) {
      return Response.json({ error: 'Text exceeds 1000 characters' }, { status: 400 });
    }

    const pair = `${source}|${target}`;
    const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${encodeURIComponent(pair)}`;
    const upstream = await fetch(url, {
      headers: { 'User-Agent': 'HtetLingo/2.0 (Cloudflare Pages)' },
    });

    if (!upstream.ok) {
      return Response.json({ error: 'Translation upstream unavailable' }, { status: 502 });
    }

    const data = (await upstream.json()) as {
      responseData?: { translatedText?: string; match?: number };
    };
    const translated = data?.responseData?.translatedText ?? '';

    return Response.json({
      translatedText: translated,
      source,
      target,
      provider: 'mymemory',
    });
  } catch {
    return Response.json({ error: 'Translation failed' }, { status: 500 });
  }
};
