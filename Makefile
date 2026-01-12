.PHONY: help build up down logs clean test

help:
	@echo "Доступные команды:"
	@echo "  make build     - Собрать все образы"
	@echo "  make up        - Запустить контейнеры"
	@echo "  make down      - Остановить контейнеры"
	@echo "  make logs      - Показать логи"
	@echo "  make clean     - Очистить всё"
	@echo "  make test      - Протестировать API"

build:
	docker-compose build

up:
	docker-compose up -d

down:
	docker-compose down

logs:
	docker-compose logs -f

clean:
	docker-compose down -v
	docker system prune -f

test:
	@echo "Тестирование API..."
	@curl -f http://localhost:8080/api/actuator/health || echo "API не доступен"
	@echo ""
	@curl -f http://localhost/api/actuator/health || echo "Frontend не доступен"

# Запуск с пересборкой
restart: down build up

# Просмотр логов конкретного сервиса
logs-backend:
	docker-compose logs -f backend

logs-postgres:
	docker-compose logs -f postgres

logs-frontend:
	docker-compose logs -f frontend

# Подключение к контейнеру
bash-backend:
	docker-compose exec backend sh

bash-postgres:
	docker-compose exec postgres psql -U postgres -d tictactoe_db

bash-frontend:
	docker-compose exec frontend sh