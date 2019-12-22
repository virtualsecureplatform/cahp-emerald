#cahp-sim grep -E '\s*x[0-9]* <=' log  
sim_dump = open('sim-dump', 'r')
cpu_dump = open('cpu-dump', 'r')

sim_line = sim_dump.readline()
cpu_line = cpu_dump.readline()
idx = 0
while sim_line:
    sim = sim_line.replace('\n', '').split(' ')
    sim_val = int(sim[2], 16)
    cpu = cpu_line.replace('\n', '').split(' ')
    cpu_val = int(cpu[4], 16)
    print('sim: {} <= {}'.format(sim[0], sim_val))
    print('cpu: {} <= {}'.format(cpu[2], cpu_val))
    if(sim[0] != cpu[2] or sim_val != cpu_val):
        print('unmatched idx:{} sim:{} cpu:{}'.format(idx, sim_val, cpu_val))
    sim_line = sim_dump.readline()
    cpu_line = cpu_dump.readline()
    idx += 1
sim_dump.close()
cpu_dump.close()
