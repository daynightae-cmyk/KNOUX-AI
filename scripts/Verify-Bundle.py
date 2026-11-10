#!/usr/bin/env python3
"""Verify distributed input files, ignoring additional Android code created later."""
from pathlib import Path
from hashlib import sha256
import json,struct,sys
root = Path(__file__).resolve().parent.parent
imgs = sorted((root/'design'/'reference').glob('*.png'))
assert len(imgs) == 10, f'Expected 10 original reference PNGs, found {len(imgs)}'
for p in imgs:
    b=p.read_bytes()
    assert b[:8] == b'\x89PNG\r\n\x1a\n', f'Invalid PNG: {p.name}'
    assert struct.unpack('>II', b[16:24])==(864,1536),(p.name,struct.unpack('>II',b[16:24]))
assert (root/'MASTER_PROMPT_CODEX_PHASE_1_UI.md').is_file()
assert len(list((root/'prompts/phase2').glob('*.md')))==10
mp=root/'provenance/MANIFEST_SHA256.json'
if '--write' in sys.argv:
    files={p.relative_to(root).as_posix():sha256(p.read_bytes()).hexdigest() for p in sorted(root.rglob('*')) if p.is_file() and p != mp}
    mp.write_text(json.dumps(files,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
else:
    files=json.loads(mp.read_text(encoding='utf-8'))
    for rel,digest in files.items():
        p=(root/rel).resolve()
        assert p.is_relative_to(root.resolve()) and p.is_file(), f'Missing/unsafe input: {rel}'
        assert sha256(p.read_bytes()).hexdigest()==digest, f'Input checksum mismatch: {rel}'
print(f'PASS: {len(imgs)} original PNGs, {len(files)} archived inputs checked; extra project files ignored')
