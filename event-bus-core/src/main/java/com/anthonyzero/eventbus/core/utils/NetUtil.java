package com.anthonyzero.eventbus.core.utils;

import com.anthonyzero.eventbus.core.exception.EventBusException;
import lombok.experimental.UtilityClass;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * net util
 *
 **/
@UtilityClass
public class NetUtil {
    // 用于缓存本地非回环IPv4地址，避免重复枚举网络接口
    private InetAddress cachedAddress;

    /**
     * 获取本地非回环IPv4地址
     *
     * @return 本地非回环IPv4地址，如果没有找到则抛出EventBusException
     */
    public InetAddress getFirstNonLoopbackIPv4Address() {
        // 检查缓存
        if (cachedAddress != null) {
            return cachedAddress;
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        // 找到合适的地址，缓存并返回
                        cachedAddress = address;
                        return address;
                    }
                }
            }
        } catch (SocketException e) {
            // 抛出更具体的异常，并包含原始异常信息
            throw new EventBusException("获取本地非回环IPv4地址失败", e);
        }

        // 如果没有找到合适的地址，抛出异常
        throw new EventBusException("获取本地非回环IPv4地址失败");
    }

    /**
     * 获取本地hostName
     *
     * @return 本地hostName
     */
    public String getHostAddr() {
        return getFirstNonLoopbackIPv4Address().getHostAddress();
    }
}
