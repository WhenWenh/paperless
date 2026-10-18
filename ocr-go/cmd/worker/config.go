package main

import (
	"errors"  // Erstellt einfache Fehlerwerte.
	"net"     // Verbindet Host und Port korrekt.
	"net/url" // Erstellt eine korrekt kodierte Verbindungs-URL.
	"os"      // Liest Umgebungsvariablen.
)

type Config struct {
	RabbitHost     string
	RabbitPort     string
	RabbitUser     string
	RabbitPassword string
	HealthAddress  string
}

func loadConfig() (Config, error) {
	// := legt eine Variable an und leitet ihren Typ automatisch ab.
	config := Config{
		RabbitHost: envOrDefault("RABBITMQ_HOST", "localhost"),

		RabbitPort: envOrDefault("RABBITMQ_PORT", "5672"),

		RabbitUser:     os.Getenv("RABBITMQ_USER"),
		RabbitPassword: os.Getenv("RABBITMQ_PASSWORD"),

		HealthAddress: ":8082",
	}

	if config.RabbitUser == "" || config.RabbitPassword == "" {
		return Config{}, errors.New(
			"RABBITMQ_USER and RABBITMQ_PASSWORD are required",
		)
	}

	return config, nil
}

func (config Config) rabbitURL() string {
	address := url.URL{
		// Verbindungsprotokoll.
		Scheme: "amqp",

		// Beispielsweise "rabbitmq:5672".
		Host: net.JoinHostPort(
			config.RabbitHost,
			config.RabbitPort,
		),

		// Kodiert auch Sonderzeichen in Zugangsdaten korrekt.
		User: url.UserPassword(
			config.RabbitUser,
			config.RabbitPassword,
		),

		// Standard-Virtual-Host "/".
		Path: "/",
	}

	// URL-Objekt in einen String umwandeln.
	return address.String()
}

// Liest einen Wert oder liefert den angegebenen Standardwert.
func envOrDefault(name, fallback string) string {
	value := os.Getenv(name)

	if value == "" {
		return fallback
	}

	return value
}
