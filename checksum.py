#!/usr/bin/python

# motor commands:
# Value: 8d 0a 16 07 18 00 00 00 04 bc d8
# Value: 8d 0a 16 07 19 00 00 00 04 bb d8
# Value: 8d 0a 16 07 1c 00 00 03 04 b5 d8
# Value: 8d 0a 16 07 1d 00 00 05 04 b2 d8
# Value: 8d 0a 16 07 2e 00 00 19 04 ab 05 d8
# Value: 8d 0a 16 07 30 00 00 1b 04 89 d8

# led commands:
# Value: 8d 0a 1a 0e 21 00 0e 00 00 00 9e d8
# Value: 8d 0a 1a 0e 22 00 0e 00 ff 00 9e d8
# Value: 8d 0a 1a 0e 1e 00 0e 00 ff 00 a2 d8

# generated: 8d:0a:1a:0e:01:00:0e:FF:FF:00:c0:d8
# from log (rear led):  8d:0a:1a:0e:17:00:01:ff:b6:d8

data1 = [ 0x8d, 0x0a, 0x16, 0x07, 0x18, 0x00, 0x00, 0x00, 0x04 ] # bc
data2 = [ 0x8d, 0x0a, 0x16, 0x07, 0x19, 0x00, 0x00, 0x00, 0x04 ] # bb
data3 = [ 0x8d, 0x0a, 0x16, 0x07, 0x1c, 0x00, 0x00, 0x03, 0x04 ] # b5
data4 = [ 0x8d, 0x0a, 0x16, 0x07, 0x1d, 0x00, 0x00, 0x05, 0x04 ] # b2
data5 = [ 0x8d, 0x0a, 0x16, 0x07, 0x2e, 0x00, 0x00, 0x19, 0x04, 0xab ] # 05
data6 = [ 0x8d, 0x0a, 0x16, 0x07, 0x30, 0x00, 0x00, 0x1b, 0x04 ] # 89

led_data1 = [ 0x8d, 0x0a, 0x1a, 0x0e, 0x21, 0x00, 0x0e, 0x00, 0x00, 0x00 ] # 9e
led_data2 = [ 0x8d, 0x0a, 0x1a, 0x0e, 0x22, 0x00, 0x0e, 0x00, 0xff, 0x00 ] # 9e
led_data3 = [ 0x8d, 0x0a, 0x1a, 0x0e, 0x1e, 0x00, 0x0e, 0x00, 0xff, 0x00 ] # a2
led_data4 = [ 0x8d, 0x0a, 0x1a, 0x0e, 0x01, 0x00, 0x0e, 0xff, 0xff, 0x00 ] # c0
rear_data_on = [ 0x8d, 0x0a, 0x1a, 0x0e, 0x17, 0x00, 0x01, 0xff ] # b6
rear_data_off = [ 0x8d, 0x0a, 0x1a, 0x0e, 0x2b, 0x00, 0x01, 0x00 ] # a1




def calc(data):
    return format((sum(data) & 0xff), '02x')

def calcModAnd(data):
    return format(((sum(data) % 256) & 0xff), '02x')

def calcModXor(data):
    return format(((sum(data) % 256) ^ 0xff), '02x')

def calcModXorAddFirst(data):
    return format((((sum(data) % 256) ^ 0xff) + data[0]) & 0xff, '02x')

def calcModXorMinusCount(data, counter):
    return format(((sum(data) % 256) ^ 0xff) - counter, '02x')



####

#print(calcModXorAddFirst(data1)) # perfect
#print(calcModXorAddFirst(data2)) # perfect
#print(calcModXorAddFirst(data3)) # perfect
#print(calcModXorAddFirst(data4)) # perfect
print(calcModXorAddFirst(data5)) # does not work
#print(calcModXorAddFirst(data6)) # perfect

#print(calcModXorAddFirst(led_data1)) # perfect
#print(calcModXorAddFirst(led_data2)) # perfect
#print(calcModXorAddFirst(led_data3)) # perfect
#print(calcModXorAddFirst(led_data4)) # perfect
#print(calcModXorAddFirst(rear_data_on)) # perfect
#print(calcModXorAddFirst(rear_data_off)) # perfect


