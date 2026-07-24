# WGCommandsAddon

Дополнительные флаги команд для WorldGuard с поддержкой нескольких команд, кулдаунов и PlaceholderAPI

## Флаги
| Флаг | От кого | Проверка |
|------|---------|----------|
| `more-cmd-player` | Игрок | ❌ Нет (все) |
| `more-cmd-console` | Консоль | ❌ Нет (все) |
| `more-perm-cmd-player` | Игрок | ✅ `wgca.use` |
| `more-perm-cmd-console` | Консоль | ✅ `wgca.use` |


## Формат
задержка_сек команда||задержка_сек команда


## Примеры

```bash
# Без задержки
/rg flag myregion more-cmd-console "say Игрок {player} вошел!"

# С задержкой 10 и 30 секунд
/rg flag myregion more-cmd-player "10 give {player} bone 1||30 give {player} diamond 1"

# С PlaceholderAPI
/rg flag myregion more-cmd-console "5 say Игрок %player_name% в {region}||60 say Привет!"
```
