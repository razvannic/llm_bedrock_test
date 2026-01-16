import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as path from 'path';

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // This points to the jar built by: ./gradlew :lambda-tools:shadowJar
    const jarPath = path.resolve(
        __dirname,
        '..',        // infra/
        '..',        // repo root
        'lambda-tools',
        'build',
        'libs',
        'lambda-tools-0.1.0.jar'
    );

    console.log("Using lambda jar:", jarPath);

    const common = {
      runtime: lambda.Runtime.JAVA_17,
      memorySize: 512,
      timeout: cdk.Duration.seconds(10),
      code: lambda.Code.fromAsset(jarPath),
    };

    new lambda.Function(this, 'GetApplicationStatusFn', {
      ...common,
      functionName: 'loan-getApplicationStatus',
      handler: 'local.dev.lambdas.GetApplicationStatusHandler::handleRequest',
    });

    new lambda.Function(this, 'UpdatePersonalFn', {
      ...common,
      functionName: 'loan-updatePersonalDetails',
      handler: 'local.dev.lambdas.UpdatePersonalDetailsHandler::handleRequest',
    });

    new lambda.Function(this, 'UpdateBusinessFn', {
      ...common,
      functionName: 'loan-updateBusinessDetails',
      handler: 'local.dev.lambdas.UpdateBusinessDetailsHandler::handleRequest',
    });

    new lambda.Function(this, 'UpdateFinancialsFn', {
      ...common,
      functionName: 'loan-updateFinancials',
      handler: 'local.dev.lambdas.UpdateFinancialsHandler::handleRequest',
    });

    new lambda.Function(this, 'SubmitApplicationFn', {
      ...common,
      functionName: 'loan-submitApplication',
      handler: 'local.dev.lambdas.SubmitApplicationHandler::handleRequest',
    });
  }
}
