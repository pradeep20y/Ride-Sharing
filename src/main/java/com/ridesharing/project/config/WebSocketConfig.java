package com.ridesharing.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.messaging.Message;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.WebSocketMessage; 

import com.ridesharing.project.config.websocket.WebSocketAuthInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketAuthInterceptor webSocketAuthInterceptor; // ← inject it

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
            config.enableSimpleBroker("/topic");
            config.setApplicationDestinationPrefixes("/app");
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
                registry.addEndpoint("/ws/ridesharing")
                    .setAllowedOriginPatterns("*")
                    .addInterceptors(new HandshakeInterceptor() {
                        @Override
                        public boolean beforeHandshake(ServerHttpRequest request,
                                                    ServerHttpResponse response,
                                                    WebSocketHandler wsHandler,
                                                    Map<String, Object> attributes) throws Exception {
                            System.out.println(">>> BEFORE HANDSHAKE: " + request.getURI());
                            return true;
                        }

                        @Override
                        public void afterHandshake(ServerHttpRequest request,
                                                ServerHttpResponse response,
                                                WebSocketHandler wsHandler,
                                                Exception exception) {
                            System.out.println(">>> AFTER HANDSHAKE exception: " + exception);
                        }
                    })
                    .withSockJS()
                    .setSessionCookieNeeded(false)
                    .setHeartbeatTime(25000);

                // registry.addEndpoint("/ws/ridesharing")
                // .setAllowedOriginPatterns("*");         
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
         
        registration.interceptors(webSocketAuthInterceptor);
            
          
    }



    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                System.out.println(">>> RAW WS OPENED: " + session.getId());
                super.afterConnectionEstablished(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                System.out.println(">>> RAW MESSAGE: " + message.getPayload());
                super.handleMessage(session, message);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                System.out.println(">>> RAW WS CLOSED: " + status);
                super.afterConnectionClosed(session, status);
            }
        });
    }
}