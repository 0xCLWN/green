// Package libswiss wraps xray-core for Android via gomobile.
// Exported names become the Kotlin/Java API: Libswiss.start(), Libswiss.stop(), etc.
package libswiss

import (
	"encoding/json"
	"github.com/0x1488/xray-core/extra"
	"strings"
	"syscall"

	"github.com/0x1488/xray-core/core"
	"github.com/0x1488/xray-core/infra/conf/serial"
	"github.com/0x1488/xray-core/transport/internet"
)

// Protector is implemented by the Android VpnService to protect outbound sockets
// from being routed back into the VPN tunnel (which would cause a loop).
type Protector interface {
	Protect(fd int32) bool
}

var instance *core.Instance

// SetProtector registers the Android socket protector. Call before Start.
func SetProtector(p Protector) error {
	return internet.RegisterDialerController(
		func(network, address string, conn syscall.RawConn) error {
			return conn.Control(func(fd uintptr) {
				p.Protect(int32(fd))
			})
		},
	)
}

// Start starts xray-core with the given JSON configuration string.
func Start(configJSON string) error {
	config, err := serial.LoadJSONConfig(strings.NewReader(configJSON))
	if err != nil {
		return err
	}
	inst, err := core.New(config)
	if err != nil {
		return err
	}
	if err := inst.Start(); err != nil {
		return err
	}
	instance = inst
	return nil
}

// Stop stops xray-core and releases resources.
func Stop() error {
	if instance == nil {
		return nil
	}
	err := instance.Close()
	instance = nil
	return err
}

// Version returns the xray-core version string.
func Version() string {
	return core.Version()
}

func VlessKeyToXrayJson(key string) (string, error) {
	cfg, err := extra.Parse(key)
	if err != nil {
		return "", err
	}
	data, err := json.MarshalIndent(cfg, "", "  ")
	if err != nil {
		return "", err
	}
	return string(data), nil
}
