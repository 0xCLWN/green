package libswiss

// Blank imports register xray-core's protocol and transport handlers via init().
// Without these, the JSON config loader won't know about the protocols.
import (
	// Core services
	_ "github.com/0x1488/xray-core/app/dispatcher"
	_ "github.com/0x1488/xray-core/app/proxyman/inbound"
	_ "github.com/0x1488/xray-core/app/proxyman/outbound"

	// Inbound protocols
	_ "github.com/0x1488/xray-core/proxy/socks"

	// Outbound protocols
	_ "github.com/0x1488/xray-core/proxy/freedom"
	_ "github.com/0x1488/xray-core/proxy/shadowsocks"
	_ "github.com/0x1488/xray-core/proxy/trojan"
	_ "github.com/0x1488/xray-core/proxy/vless/outbound"
	_ "github.com/0x1488/xray-core/proxy/vmess/outbound"

	// Transports
	_ "github.com/0x1488/xray-core/transport/internet/grpc"
	_ "github.com/0x1488/xray-core/transport/internet/reality"
	_ "github.com/0x1488/xray-core/transport/internet/tcp"
	_ "github.com/0x1488/xray-core/transport/internet/tls"
	_ "github.com/0x1488/xray-core/transport/internet/websocket"
)
