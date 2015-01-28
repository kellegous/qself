require 'yaml'

PATHS = [
  'lib',
  'dep',
]

CONFIG = YAML.load_file 'config.yaml'

CLEAN = FileList[]

env = ENV.clone.update({
  # Setup GOPATH
  'GOPATH' => PATHS.map { |x| File.join(Dir.pwd, x) }.join(':')
})

# A file task that marks the target for removal during clean.
def file!(*args, &block)
  task = Rake::FileTask.define_task(*args, &block)
  CLEAN.include(task.name)
  task
end

# A clean task that removes everything that was added to CLEAN
task :clean do
  CLEAN.each { |f| rm_r(f) rescue nil }
end

file! 'bin/agent' => FileList['agent/*', 'lib/**/*', 'dep/**/*'] do
  sh 'go', 'build', '-o', 'bin/agent', 'agent/agent.go'
end

file! 'bin/sensor' => FileList['sensor/*', 'lib/**/*', 'dep/**/*'] do
  sh 'go', 'build', '-o', 'bin/sensor', 'sensor/host.go'
end

task :subl do
  spawn('subl', '.')
end

task :deploy do
  # TODO(knorton):
  # => rsync needed code.
  # => use rake to bootstrap.
  # => provide a mechanism for replacing the proc.
  CONFIG['agents'].each { |agent|
    sh 'rsync', '-r', 'lib', 'agent', "#{agent}:/tmp/qself"
  }
end

task :default => ['bin/agent', 'bin/sensor']

task :debug do
  puts CONFIG.inspect
end