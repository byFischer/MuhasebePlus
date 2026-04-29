import { useEffect, useState } from "react"
import { Flex, Text, Box, Input, VStack } from "@chakra-ui/react"
import { Search, Bell } from "lucide-react"
import { ColorModeButton } from "@/components/ui/color-mode"
import { useColorModeValue } from "@/components/ui/color-mode"
import api from "@/services/api"

interface NotificationItem {
  notificationId: number
  notificationType: string
  subject: string
  message: string
  sentAt: string
  read: boolean
}

function formatNotificationDate(value: string): string {
  if (!value) return ""
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  return date.toLocaleDateString("tr-TR", { day: "2-digit", month: "short" })
}

export function Header() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isNotificationsOpen, setNotificationsOpen] = useState(false)

  const bg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const inputBg = useColorModeValue("#f4f4f5", "#1a1a1a")
  const iconColor = useColorModeValue("#a1a1aa", "#71717a")
  const menuBg = useColorModeValue("#ffffff", "#111111")
  const hoverBg = useColorModeValue("#fafafa", "#18181b")

  async function loadNotifications() {
    try {
      const [countRes, notificationsRes] = await Promise.all([
        api.get("/api/notifications/unread-count"),
        api.get("/api/notifications"),
      ])
      setUnreadCount(countRes.data?.count ?? 0)
      setNotifications((notificationsRes.data ?? []).slice(0, 5))
    } catch (err) {
      console.error("Notification fetch error:", err)
      setUnreadCount(0)
      setNotifications([])
    }
  }

  useEffect(() => {
    loadNotifications()
  }, [])

  async function handleNotificationClick(notification: NotificationItem) {
    try {
      if (!notification.read) {
        await api.patch(`/api/notifications/${notification.notificationId}/read`)
        await loadNotifications()
      }
    } catch (err) {
      console.error("Notification read error:", err)
    }
  }

  return (
    <Flex
      as="header"
      align="center"
      justify="space-between"
      px="6"
      py="3"
      bg={bg}
      borderBottom="1px solid"
      borderColor={borderColor}
      minH="56px"
      flexShrink={0}
    >
      <Box>
        <Text fontSize="sm" fontWeight="600" color={textPrimary}>
          Gösterge Paneli
        </Text>
        <Text fontSize="xs" color={textMuted}>
          30 Mart 2026, Pazartesi
        </Text>
      </Box>

      <Flex align="center" gap="3">
        {/* Search */}
        <Flex
          align="center"
          gap="2"
          bg={inputBg}
          px="3"
          py="1.5"
          borderRadius="md"
          width="220px"
        >
          <Search size={14} color={iconColor} />
          <Input
            placeholder="Ara..."
            fontSize="sm"
            color={textPrimary}
            _placeholder={{ color: textMuted }}
            height="auto"
            p="0"
            border="none"
            outline="none"
            bg="transparent"
            _focus={{ outline: "none", boxShadow: "none" }}
          />
        </Flex>

        {/* Notification */}
        <Box position="relative">
          <Flex
            align="center"
            justify="center"
            width="36px"
            height="36px"
            borderRadius="md"
            cursor="pointer"
            position="relative"
            transition="all 0.15s ease"
            _hover={{ bg: inputBg }}
            onClick={() => {
              setNotificationsOpen((open) => !open)
              loadNotifications()
            }}
            aria-label="Notifications"
          >
            <Bell size={16} color={iconColor} />
            {unreadCount > 0 && (
              <Box
                position="absolute"
                top="7px"
                right="7px"
                minW="7px"
                height="7px"
                borderRadius="full"
                bg="#dc2626"
              />
            )}
          </Flex>

          {isNotificationsOpen && (
            <Box
              position="absolute"
              top="44px"
              right="0"
              width="320px"
              maxW="calc(100vw - 24px)"
              bg={menuBg}
              border="1px solid"
              borderColor={borderColor}
              borderRadius="md"
              boxShadow="lg"
              zIndex="dropdown"
              overflow="hidden"
            >
              <Flex align="center" justify="space-between" px="4" py="3" borderBottom="1px solid" borderColor={borderColor}>
                <Text fontSize="sm" fontWeight="600" color={textPrimary}>Bildirimler</Text>
                {unreadCount > 0 && (
                  <Text fontSize="xs" fontWeight="600" color="#dc2626" className="tabular-nums">
                    {unreadCount}
                  </Text>
                )}
              </Flex>

              {notifications.length === 0 ? (
                <Flex align="center" justify="center" minH="96px">
                  <Text fontSize="sm" color={textMuted}>Henuz bildirim yok</Text>
                </Flex>
              ) : (
                <VStack align="stretch" gap="0" maxH="320px" overflowY="auto">
                  {notifications.map((notification) => (
                    <Box
                      key={notification.notificationId}
                      px="4"
                      py="3"
                      cursor="pointer"
                      bg={notification.read ? "transparent" : inputBg}
                      borderBottom="1px solid"
                      borderColor={borderColor}
                      _hover={{ bg: hoverBg }}
                      onClick={() => handleNotificationClick(notification)}
                    >
                      <Flex justify="space-between" gap="3" mb="1">
                        <Text fontSize="sm" fontWeight="600" color={textPrimary} lineClamp={1}>
                          {notification.subject}
                        </Text>
                        <Text fontSize="xs" color={textMuted} whiteSpace="nowrap">
                          {formatNotificationDate(notification.sentAt)}
                        </Text>
                      </Flex>
                      <Text fontSize="xs" color={textMuted} lineClamp={2}>
                        {notification.message}
                      </Text>
                    </Box>
                  ))}
                </VStack>
              )}
            </Box>
          )}
        </Box>

        {/* Theme Toggle */}
        <ColorModeButton
          borderRadius="md"
          size="sm"
          color={iconColor}
          _hover={{ bg: inputBg }}
        />

        {/* Avatar */}
        <Flex
          align="center"
          justify="center"
          width="32px"
          height="32px"
          borderRadius="full"
          bg={useColorModeValue("#09090b", "#fafafa")}
          color={useColorModeValue("#fafafa", "#09090b")}
          fontSize="xs"
          fontWeight="600"
          cursor="pointer"
          flexShrink={0}
        >
          EZ
        </Flex>
      </Flex>
    </Flex>
  )
}
