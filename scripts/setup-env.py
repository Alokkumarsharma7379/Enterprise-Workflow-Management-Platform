"""Generate local secrets without overwriting an existing environment file."""
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
target = root / '.env'
content = (root / '.env.example').read_text(encoding='utf-8')
content = content.replace('replace-with-a-random-database-password', secrets.token_urlsafe(32))
content = content.replace('replace-with-at-least-32-random-bytes', secrets.token_urlsafe(48))
try:
    with target.open('x', encoding='utf-8') as file:
        file.write(content)
    print('Created .env with random secrets. Keep this file private.')
except FileExistsError:
    print('.env already exists; left unchanged.')
